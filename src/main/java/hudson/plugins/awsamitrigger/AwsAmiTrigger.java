package hudson.plugins.awsamitrigger;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.model.BuildableItem;
import hudson.model.Item;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.ListBoxModel;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;

import jenkins.model.Jenkins;

public final class AwsAmiTrigger extends Trigger<BuildableItem> {

  private static final Logger LOGGER = Logger.getLogger(AwsAmiTrigger.class.getName());

  private final String credentialsId;
  private final String regionName;
  private final String pattern;
  private final String lastRun;
  private final String lastMatch;

  private EC2Service ec2Service;

  /**
   * Create a new {@link AwsAmiTrigger}.
   *
   * @param spec
   *          crontab specification that defines how often to poll
   * @param credentialsId
   *          aws credentials
   * @param regionName
   *          aws region name
   * @param pattern
   *          pattern for ami name
   *
   * @throws ANTLRException
   *           if unable to parse the crontab specification
   */
  @DataBoundConstructor
  public AwsAmiTrigger(String spec, String credentialsId, String regionName, String pattern)
      throws ANTLRException {
    super(spec);

    this.credentialsId = credentialsId;
    this.regionName = regionName;
    this.pattern = pattern;
    this.lastRun = "";
    this.lastMatch = "";
  }

  private synchronized EC2Service getEc2Service() {
    if(ec2Service == null) {
      ec2Service = new EC2Service(credentialsId, regionName);
    }
    return ec2Service;
  }

  private AmazonEC2Client getAmazonEC2Client() {
    return getEc2Service().getAmazonEC2Client();
  }

  @Override
  public void run() {
    LOGGER.log(Level.INFO, "Running..." + toString());
  }

  public String getCredentialsId() {
    return credentialsId;
  }

  public String getRegionName() {
      return regionName;
 }

  public String getPattern() {
    return pattern;
  }

  @Extension
  public static final class DescriptorImpl extends TriggerDescriptor {

    @Override
    public boolean isApplicable(Item item) {
      return item instanceof BuildableItem;
    }

    public ListBoxModel doFillCredentialsIdItems() {
      return AWSCredentialsHelper.doFillCredentialsIdItems(Jenkins.getActiveInstance());
    }

    @Override
    public String getDisplayName() {
      return Messages.DisplayName();
    }

    public ListBoxModel doFillRegionNameItems() {
      final ListBoxModel options = new ListBoxModel();
      for(Region region : RegionUtils.getRegions()) {
        options.add(region.getName());
      }
      return options;
    }
  }
}
