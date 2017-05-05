package hudson.plugins.awsamitrigger;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

public final class EC2Service {
  private static final Logger LOGGER = Logger.getLogger(EC2Service.class.getName());

  private String credentialsId;
  private String regionName;

  public EC2Service(String credentialsId, String regionName) {
    super();
    this.credentialsId = credentialsId;
    this.regionName = regionName;
  }

  AmazonEC2Client getAmazonEC2Client() {
    final AmazonEC2Client client;

    ProxyConfiguration proxy = Jenkins.getInstance().proxy;
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    if(proxy != null) {
      clientConfiguration.setProxyHost(proxy.name);
      clientConfiguration.setProxyPort(proxy.port);
      clientConfiguration.setProxyUsername(proxy.getUserName());
      clientConfiguration.setProxyPassword(proxy.getPassword());
    }

    AmazonWebServicesCredentials credentials = getCredentials(credentialsId);
    if(credentials == null) {
      client = new AmazonEC2Client(clientConfiguration);
    } else {
      if(LOGGER.isLoggable(Level.FINE)) {
        String awsAccessKeyId = credentials.getCredentials().getAWSAccessKeyId();
        String obfuscatedAccessKeyId = StringUtils.left(awsAccessKeyId, 4) + StringUtils.repeat("*", awsAccessKeyId.length() - (2 * 4)) + StringUtils.right(awsAccessKeyId, 4);
        LOGGER.log(Level.FINE, "Connect to Amazon ECS with IAM Access Key {1}", new Object[]{obfuscatedAccessKeyId});
      }
      client = new AmazonEC2Client(credentials, clientConfiguration);
    }
    client.setRegion(getRegion(regionName));
    LOGGER.log(Level.FINE, "Selected Region: {0}", regionName);

    return client;
  }

  Region getRegion(String regionName) {
    if(StringUtils.isNotEmpty(regionName)) {
        return RegionUtils.getRegion(regionName);
    } else {
        return Region.getRegion(Regions.US_EAST_1);
    }
  }

  private AmazonWebServicesCredentials getCredentials(String credentialsId) {
    return AWSCredentialsHelper.getCredentials(credentialsId, Jenkins.getActiveInstance());
  }

  // state is available
  public DescribeImagesResult describeImages(String filter) {
    final AmazonEC2Client client = getAmazonEC2Client();
    final DescribeImagesRequest request = new DescribeImagesRequest();
    request.setFilters(Collections.singleton(new Filter("name",Collections.singletonList(filter))));
    return client.describeImages(request);
  }
}
