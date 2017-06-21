/**
  * MIT License
  *
  * Copyright (c) 2017 Rik Turnbull
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in all
  * copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */
package hudson.plugins.awsamitrigger;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.util.DateUtils;

import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;

import hudson.ProxyConfiguration;

import jenkins.model.Jenkins;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

/**
 * AWS EC2 client.
 *
 * @author Rik Turnbull
 *
 */
public class EC2Service {
  private static final Logger LOGGER = Logger.getLogger(EC2Service.class.getName());

  private AmazonEC2Client client;

  private String credentialsId;
  private String regionName;

  /**
   * Creates a new {@link EC2Service}.
   *
   * @param credentialsId  AWS credentials identifier
   * @param regionName     AWS region name
   */
  public EC2Service(String credentialsId, String regionName) {
    super();
    this.credentialsId = credentialsId;
    this.regionName = regionName;
  }

  /**
   * Returns an {@link AmazonEC2Client}.
   * @return {@link AmazonEC2Client} singleton using the <code>credentialsId</code>
   * and <code>regionName</code>
   */
  private synchronized AmazonEC2Client getAmazonEC2Client() {
    if(client == null) {
      ClientConfiguration clientConfiguration = new ClientConfiguration();
      Jenkins jenkins = Jenkins.getInstance();
      if(jenkins != null) {
        ProxyConfiguration proxy = jenkins.proxy;
        if(proxy != null) {
          clientConfiguration.setProxyHost(proxy.name);
          clientConfiguration.setProxyPort(proxy.port);
          clientConfiguration.setProxyUsername(proxy.getUserName());
          clientConfiguration.setProxyPassword(proxy.getPassword());
        }
      }

      AmazonWebServicesCredentials credentials = getAWSCredentials(credentialsId);
      if(credentials == null) {
        client = new AmazonEC2Client(clientConfiguration);
      } else {
        client = new AmazonEC2Client(credentials, clientConfiguration);
      }
      client.setRegion(getRegion(regionName));
    }
    return client;
  }

  /**
   * Gets AWS region.
   *
   * @param regionName AWS region name
   * @return AWS region for <code>regionName</code> or US_EAST_1
   */
  private Region getRegion(String regionName) {
    Region region = RegionUtils.getRegion(regionName);
    if(region == null) {
      region = Region.getRegion(Regions.US_EAST_1);
    }
    return region;
  }

  /**
   * Gets AWS credentials.
   *
   * @param credentialsId Jenkins credentials identifier
   * @return AWS credentials for <code>credentialsId</code> that can be used
   * for AWS calls
   */
  private AmazonWebServicesCredentials getAWSCredentials(String credentialsId) {
    return AWSCredentialsHelper.getCredentials(credentialsId, Jenkins.getActiveInstance());
  }

  /**
   * Fetches a list of images matching the supplied <code>filters</code>. The
   * list is returned sorted by <code>creationDate</code> with the newest
   * item at the beginning of the list.
   *
   * @param filters   collection of AWS <code>Filter</code>
   * @return a list of AWS images sorted in reverse order by <code>creationDate</code>
   */
  public List<Image> describeImages(Collection<Filter> filters) {
    final AmazonEC2Client client = getAmazonEC2Client();

    final DescribeImagesRequest request = new DescribeImagesRequest();
    request.setFilters(filters);

    final List<Image> images = client.describeImages(request).getImages();
    Collections.sort(images, new Comparator<Image>() {
      @Override
      public int compare(Image a, Image b) {
        String aDate = a.getCreationDate();
        String bDate = b.getCreationDate();
        if(StringUtils.isEmpty(aDate) || StringUtils.isEmpty(bDate)) {
          return bDate.compareTo(aDate);
        } else {
          return DateUtils.parseISO8601Date(bDate).compareTo(DateUtils.parseISO8601Date(aDate));
        }
      }
    });
    return images;
  }

  /**
   * Fetches the latest image matching the supplied <code>filters</code>.
   *
   * @param filters   collection of AWS <code>Filter</code>
   * @return the latest AWS image matching the <code>filters</code>
   */
  public Image fetchLatestImage(Collection<Filter> filters) {
    Image image = null;

    List<Image> images = describeImages(filters);
    if(!images.isEmpty()) {
      image = images.get(0);
    }

    return image;
  }
}
