/*
  MIT License

  Copyright (c) 2017 Rik Turnbull

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
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

public final class EC2Service {
  private static final Logger LOGGER = Logger.getLogger(EC2Service.class.getName());

  private String credentialsId;
  private String regionName;

  public EC2Service(String credentialsId, String regionName) {
    super();
    this.credentialsId = credentialsId;
    this.regionName = regionName;
  }

  private AmazonEC2Client getAmazonEC2Client() {
    final AmazonEC2Client client;

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

    AmazonWebServicesCredentials credentials = getCredentials(credentialsId);
    if(credentials == null) {
      client = new AmazonEC2Client(clientConfiguration);
    } else {
      client = new AmazonEC2Client(credentials, clientConfiguration);
    }
    client.setRegion(getRegion(regionName));

    return client;
  }

  private Region getRegion(String regionName) {
    if(StringUtils.isNotEmpty(regionName)) {
        return RegionUtils.getRegion(regionName);
    } else {
        return Region.getRegion(Regions.US_EAST_1);
    }
  }

  private AmazonWebServicesCredentials getCredentials(String credentialsId) {
    return AWSCredentialsHelper.getCredentials(credentialsId, Jenkins.getActiveInstance());
  }

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
}
