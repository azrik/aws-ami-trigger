package hudson.plugins.awsamitrigger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import javax.servlet.ServletException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.util.DateUtils;

import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsHelper;

import antlr.ANTLRException;

import hudson.Extension;
import hudson.model.BuildableItem;
import hudson.model.Item;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public final class AwsAmiTrigger extends Trigger<BuildableItem> {

  private final static int MAX_TEST_IMAGES = 10;
  private final static Logger LOGGER = Logger.getLogger(AwsAmiTrigger.class.getName());
  private final static Pattern tagsPattern =  Pattern.compile("^:[^=]+=[^=]+.*");

  private final String credentialsId;
  private final String regionName;
  private final List<AwsAmiTriggerFilter> filters;
  private Date lastRun;

  private transient EC2Service ec2Service;

  /**
   * Create a new {@link AwsAmiTrigger}.
   *
   * @param spec
   *          crontab specification that defines how often to poll
   * @param credentialsId
   *          aws credentials
   * @param regionName
   *          aws region name
   * @param filters
   *          list of filters
   *
   * @throws ANTLRException
   *           if unable to parse the crontab specification
   */
  @DataBoundConstructor
  public AwsAmiTrigger(String spec, String credentialsId, String regionName, List<AwsAmiTriggerFilter> filters)
      throws ANTLRException {
    super(spec);

    this.credentialsId = credentialsId;
    this.regionName = regionName;
    this.filters = filters;
    this.lastRun = new Date();
  }

  private synchronized EC2Service getEc2Service() {
    if(ec2Service == null) {
      ec2Service = new EC2Service(credentialsId, regionName);
    }
    return ec2Service;
  }

  @Override
  public void run() {
    for(AwsAmiTriggerFilter filter : filters) {
      List<Image> images = getEc2Service().describeImages(filter.toAWSFilters());
      if(!images.isEmpty()) {
        if(DateUtils.parseISO8601Date(images.get(0).getCreationDate()).compareTo(lastRun) >= 0) {
          LOGGER.log(Level.INFO, "Triggering build for new image: " + images.get(0).toString());
          lastRun = new Date();
          try {
            job.save();
            job.scheduleBuild(new AwsAmiTriggerCause(this, images.get(0)));
          } catch(IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save last run time, build not triggered", e);
          }
        }
      }
    }
  }

  public String getCredentialsId() {
    return credentialsId;
  }

  public String getRegionName() {
      return regionName;
 }

  public List<AwsAmiTriggerFilter> getFilters() {
    return filters;
  }

  @Extension
  public static final class DescriptorImpl extends TriggerDescriptor {

    @Override
    public boolean isApplicable(Item item) {
      return item instanceof BuildableItem;
    }

    @Override
    public String getDisplayName() {
      return Messages.DisplayName();
    }

    public ListBoxModel doFillCredentialsIdItems() {
      return AWSCredentialsHelper.doFillCredentialsIdItems(Jenkins.getActiveInstance());
    }

    public ListBoxModel doFillRegionNameItems() {
      final ListBoxModel options = new ListBoxModel();
      final List<String> regionNames = new ArrayList<String>();
      final List<Region> regions = RegionUtils.getRegions();
      for(Region region : regions) {
        regionNames.add(region.getName());
      }
      Collections.sort(regionNames);
      for(String regionName : regionNames) {
        options.add(regionName);
      }
      return options;
    }

    public ListBoxModel doFillArchitectureItems() {
      final ListBoxModel options = new ListBoxModel();
      options.add(AwsAmiTriggerFilter.ANY);
      options.add("i386");
      options.add("x86_64");
      return options;
    }

    public ListBoxModel doFillOwnerAliasItems() {
      final ListBoxModel options = new ListBoxModel();
      options.add(AwsAmiTriggerFilter.ANY);
      options.add("amazon");
      options.add("aws-marketplace");
      options.add("microsoft");
      return options;
    }

    public ListBoxModel doFillSharedItems() {
      final ListBoxModel options = new ListBoxModel();
      options.add(AwsAmiTriggerFilter.ANY);
      options.add("true");
      options.add("false");
      return options;
    }

    public FormValidation doCheckName(@QueryParameter String name, @QueryParameter String description, @QueryParameter String tags) {
      if("*".equals(StringUtils.trim(name)) && StringUtils.isEmpty(description) && StringUtils.isEmpty(tags)) {
        return FormValidation.error(Messages.WildcardTooWild());
      }
      return checkMinimum(name, description, tags);
    }

    public FormValidation doCheckDescription(@QueryParameter String description, @QueryParameter String name, @QueryParameter String tags) {
      if("*".equals(StringUtils.trim(description)) && StringUtils.isEmpty(name) && StringUtils.isEmpty(tags)) {
        return FormValidation.error(Messages.WildcardTooWild());
      }
      return checkMinimum(name, description, tags);
    }

    public FormValidation doCheckTags(@QueryParameter String tags, @QueryParameter String name, @QueryParameter String description) {
      if(!StringUtils.isEmpty(tags)) {
        final Matcher matcher = tagsPattern.matcher(tags);
        if(!matcher.matches()) {
          return FormValidation.error(Messages.InvalidTagsSpecification());
        }
      }
      return checkMinimum(name, description, tags);
    }

    private FormValidation checkMinimum(String name, String description, String tags) {
      if(StringUtils.isEmpty(name) && StringUtils.isEmpty(description) && StringUtils.isEmpty(tags)) {
        return FormValidation.error(Messages.CheckMinimum());
      }
      return FormValidation.ok();
    }

    public FormValidation doTestFilter(@QueryParameter("credentialsId") final String testCredentialsId,
                                       @QueryParameter("regionName") final String testRegionName,
                                       @QueryParameter("architecture") final String testArchitecture,
                                       @QueryParameter("description") final String testDescription,
                                       @QueryParameter("name") final String testName,
                                       @QueryParameter("ownerAlias") final String testOwnerAlias,
                                       @QueryParameter("ownerId") final String testOwnerId,
                                       @QueryParameter("productCode") final String testProductCode,
                                       @QueryParameter("tags") final String testTags,
                                       @QueryParameter("shared") final String testShared
                                       ) throws IOException, ServletException {
      final StringBuffer testResults = new StringBuffer();
      final EC2Service testEc2Service = new EC2Service(testCredentialsId, testRegionName);
      final AwsAmiTriggerFilter testFilter = new AwsAmiTriggerFilter(testArchitecture, testDescription, testName, testOwnerAlias, testOwnerId, testProductCode, testTags, testShared);

      try {
        final List<Image> testImages = testEc2Service.describeImages(testFilter.toAWSFilters());
        final int testImagesCount = testImages.size();
        final int testImagesMax = Math.min(testImagesCount, MAX_TEST_IMAGES);
        if(testImagesCount <= MAX_TEST_IMAGES) {
          testResults.append(Messages.MatchedImages(testImagesCount));
        } else {
          testResults.append(Messages.MatchedImagesLimit(testImagesCount, MAX_TEST_IMAGES));
        }
        testResults.append("\n");
        if(testImagesMax > 0) {
          for(Image testImage : testImages.subList(0, testImagesMax)) {
            testResults.append(testImage.getCreationDate() + " " + testImage.getImageId() + " " + testImage.getName() + "\n");
          }
        }
        return FormValidation.ok(testResults.toString());
      } catch(AmazonClientException e) {
        return FormValidation.error(e.getMessage());
      }
    }
  }
}
