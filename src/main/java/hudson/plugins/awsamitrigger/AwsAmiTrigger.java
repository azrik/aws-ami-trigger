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

import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * A Jenkins {@link hudson.triggers.Trigger} for AWS AMIs.
 *
 * @author Rik Turnbull
 *
 */
public final class AwsAmiTrigger extends Trigger<BuildableItem> {

  private final static int MAX_TEST_IMAGES = 10;
  private final static Logger LOGGER = Logger.getLogger(AwsAmiTrigger.class.getName());
  private final static Pattern tagsPattern =  Pattern.compile("^[^=]+=[^=]+.*");

  private final String credentialsId;
  private final String regionName;
  private final List<AwsAmiTriggerFilter> filters;
  private Date lastRun;

  private transient EC2Service ec2Service;

  /**
   * Creates a new {@link AwsAmiTrigger}.
   *
   * @param spec            crontab specification that defines how often to poll
   * @param credentialsId   aws credentials id
   * @param regionName      aws region name
   * @param filters         list of filters
   * @throws ANTLRException if unable to parse the crontab specification
   */
  @DataBoundConstructor
  public AwsAmiTrigger(String spec, String credentialsId, String regionName, List<AwsAmiTriggerFilter> filters)
      throws ANTLRException {
    super(spec);

    this.credentialsId = credentialsId;
    this.regionName = regionName;
    this.filters = filters;
    this.lastRun = new Date();
    LOGGER.log(Level.INFO, "constructor:" + toString());
  }

  /**
   * Returns an {@link EC2Service}.
   * @return {@link EC2Service} singleton using the <code>credentialsId</code>
   * and <code>regionName</code>
   */
  private synchronized EC2Service getEc2Service() {
    if(ec2Service == null) {
      ec2Service = new EC2Service(credentialsId, regionName);
    }
    return ec2Service;
  }

  /**
   * Checks for new AMIs since the last run. A job is scheduled for each
   * filter that has a new AMI.
   */
  @Override
  public void run() {
    LOGGER.log(Level.INFO, "run:" + toString());
    try {
      for(AwsAmiTriggerFilter filter : filters) {
        List<Image> images = getEc2Service().describeImages(filter.toAWSFilters());
        if(!images.isEmpty()) {
          if(DateUtils.parseISO8601Date(images.get(0).getCreationDate()).compareTo(lastRun) >= 0) {
            LOGGER.log(Level.INFO, Messages.TriggeringBuild(images.get(0).toString()));
            lastRun = new Date();
            job.save();
            boolean scheduled = job.scheduleBuild(new AwsAmiTriggerCause(this, images.get(0)));
            LOGGER.log(Level.INFO, String.valueOf(scheduled));
            LOGGER.log(Level.INFO, job.getName());
          }
        }
      }
    } catch(IOException e) {
      LOGGER.log(Level.WARNING, Messages.TriggeringFailed(), e);
    }
  }

  /**
   * Gets AWS credentials identifier.
   * @return AWS credentials identifier
   */
  public String getCredentialsId() {
    return credentialsId;
  }

  /**
   * Gets AWS region name.
   * @return AWS region name
   */
  public String getRegionName() {
      return regionName;
  }

  /**
   * Gets {@link AwsAmiTriggerFilter} filters.
   * @return {@link AwsAmiTriggerFilter} filters
   */
  public List<AwsAmiTriggerFilter> getFilters() {
    return filters;
  }

  /**
   * Converts {@link AwsAmiTrigger} into a <code>String</code>
   * representation.
   *
   * @return string containing all fields
   */
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("credentialsId", credentialsId)
      .append("regionName", regionName)
      .append("lastRun", lastRun)
      .append("filters", filters).toString();
  }

  /**
   * A Jenkins <code>DescriptorImpl</code> for the {@link AwsAmiTrigger}.
   *
   * @author Rik Turnbull
   *
   */
  @Extension
  public static final class DescriptorImpl extends TriggerDescriptor {

    /**
     * Returns the applicability of this trigger.
     * @return true if the {@link hudson.model.Item} is a
     * {@link hudson.model.BuildableItem}
     */
    @Override
    public boolean isApplicable(Item item) {
      return item instanceof BuildableItem;
    }

    /**
     * Returns the trigger display name.
     * @return a one line description of the {@link AwsAmiTrigger}
     */
    @Override
    public String getDisplayName() {
      return Messages.DisplayName();
    }

    /**
     * Returns a list of AWS credentials identifiers.
     * @return {@link ListBoxModel} populated with AWS credential identifiers
     */
    public ListBoxModel doFillCredentialsIdItems() {
      return AWSCredentialsHelper.doFillCredentialsIdItems(Jenkins.getActiveInstance());
    }

    /**
     * Returns a list of AWS region names.
     * @return {@link ListBoxModel} populated with AWS region names
     */
    public ListBoxModel doFillRegionNameItems() {
      final ListBoxModel options = new ListBoxModel();
      final List<String> regionNames = new ArrayList<String>();
      final List<Region> regions = RegionUtils.getRegions();
      for(Region region : regions) {
        regionNames.add(region.getName());
      }
      Collections.sort(regionNames);
      options.add("- select -");
      for(String regionName : regionNames) {
        options.add(regionName);
      }
      return options;
    }

    /**
     * Returns a list of AMI architectures.
     * @return {@link ListBoxModel} populated with AMI architecture options
     */
    public ListBoxModel doFillArchitectureItems() {
      final ListBoxModel options = new ListBoxModel();
      options.add(AwsAmiTriggerFilter.ANY);
      options.add("i386");
      options.add("x86_64");
      return options;
    }

    /**
     * Returns a list of AMI owner aliases.
     * @return {@link ListBoxModel} populated with AMI owner alias options
     */
    public ListBoxModel doFillOwnerAliasItems() {
      final ListBoxModel options = new ListBoxModel();
      options.add(AwsAmiTriggerFilter.ANY);
      options.add("amazon");
      options.add("aws-marketplace");
      options.add("microsoft");
      return options;
    }

    /**
     * Returns a list of AMI is-public options.
     * @return {@link ListBoxModel} populated with AMI is-public options
     */
    public ListBoxModel doFillSharedItems() {
      final ListBoxModel options = new ListBoxModel();
      options.add(AwsAmiTriggerFilter.ANY);
      options.add("true");
      options.add("false");
      return options;
    }

    /**
     * Validates the filter <code>name</code>.
     *
     * @param name           AMI name pattern
     * @param description    AMI description pattern
     * @param tags           AMI tags
     * @return FormValidation. ok if valid or FormValidation.error and an error
     * message otherwise
     */
    public FormValidation doCheckName(@QueryParameter String name, @QueryParameter String description, @QueryParameter String tags) {
      if("*".equals(StringUtils.trim(name)) && StringUtils.isEmpty(description) && StringUtils.isEmpty(tags)) {
        return FormValidation.error(Messages.WildcardTooWild());
      }
      return checkMinimum(name, description, tags);
    }

    /**
     * Validates the filter <code>description</code>.
     *
     * @param description    AMI description pattern
     * @param name           AMI name pattern
     * @param tags           AMI tags
     * @return FormValidation. ok if valid or FormValidation.error and an error
     * message otherwise
     */
    public FormValidation doCheckDescription(@QueryParameter String description, @QueryParameter String name, @QueryParameter String tags) {
      if("*".equals(StringUtils.trim(description)) && StringUtils.isEmpty(name) && StringUtils.isEmpty(tags)) {
        return FormValidation.error(Messages.WildcardTooWild());
      }
      return checkMinimum(name, description, tags);
    }

    /**
     * Validates the filter <code>tags</code>.
     *
     * @param tags           AMI tags
     * @param name           AMI name pattern
     * @param description    AMI description pattern
     * @return FormValidation. ok if valid or FormValidation.error and an error
     * message otherwise
     */
    public FormValidation doCheckTags(@QueryParameter String tags, @QueryParameter String name, @QueryParameter String description) {
      if(!StringUtils.isEmpty(tags)) {
        final Matcher matcher = tagsPattern.matcher(tags);
        if(!matcher.matches()) {
          return FormValidation.error(Messages.InvalidTagsSpecification());
        }
      }
      return checkMinimum(name, description, tags);
    }

    /**
     * Checks that at least one of <code>name</code>, <code>description</code>
     * or <code>tags</code> has a value.
     * @param name           AMI name pattern
     * @param description    AMI description pattern
     * @param tags           AMI tags
     * @return FormValidation. ok if valid or FormValidation.error and an error
     * message otherwise
     **/
    private FormValidation checkMinimum(String name, String description, String tags) {
      if(StringUtils.isEmpty(name) && StringUtils.isEmpty(description) && StringUtils.isEmpty(tags)) {
        return FormValidation.error(Messages.CheckMinimum());
      }
      return FormValidation.ok();
    }

    /**
     * Tests the filter via the AWS EC2 service.
     *
     * @param testCredentialsId    AWS credentials id
     * @param testRegionName       AWS region name
     * @param testArchitecture     AMI architecture
     * @param testDescription      AMI description pattern
     * @param testName             AMI name pattern
     * @param testOwnerAlias       AMI owner alias
     * @param testOwnerId          AMI owner id
     * @param testProductCode      AMI product code
     * @param testTags             AMI tags
     * @param testShared           AMI is-public indicator
     * @return FormValidation.ok and up to 10 matching AMIs (creationDate, imageId, Name)
     *         or FormValidation.error if an AWS exception occurred
     * @throws IOException if IO error occurred
     * @throws ServletException if servlet error occurred
     */
    public FormValidation doTestFilter(@QueryParameter("credentialsId") final String testCredentialsId,
                                       @QueryParameter("regionName") final String testRegionName,
                                       @QueryParameter("architecture") final String testArchitecture,
                                       @QueryParameter("description") final String testDescription,
                                       @QueryParameter("name") final String testName,
                                       @QueryParameter("ownerAlias") final String testOwnerAlias,
                                       @QueryParameter("ownerId") final String testOwnerId,
                                       @QueryParameter("productCode") final String testProductCode,
                                       @QueryParameter("tags") final String testTags,
                                       @QueryParameter("shared") final String testShared) throws IOException, ServletException {
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
        testResults.append(CharUtils.LF);
        if(testImagesMax > 0) {
          for(Image testImage : testImages.subList(0, testImagesMax)) {
            testResults.append(testImage.getCreationDate());
            testResults.append(CharUtils.LF);
            testResults.append(testImage.getImageId());
            testResults.append(" ");
            testResults.append(testImage.getName());
            testResults.append(CharUtils.LF);
            if(testImage.getDescription() != null) {
              testResults.append(testImage.getDescription());
              testResults.append(CharUtils.LF);
            }
            testResults.append(CharUtils.LF);
          }
        }
        return FormValidation.ok(testResults.toString());
      } catch(AmazonClientException e) {
        return FormValidation.error(e.getMessage());
      }
    }
  }
}
