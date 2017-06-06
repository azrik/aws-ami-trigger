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

import com.amazonaws.services.ec2.model.Image;

import hudson.EnvVars;
import hudson.model.Cause;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Cause for triggering {@link AwsAmiTrigger}.
 *
 * @author Rik Turnbull
 *
 */
public final class AwsAmiTriggerCause extends Cause {

  private final List<AwsAmiTriggerMatch> matches = new ArrayList<AwsAmiTriggerMatch>();

  /**
   * Creates a new {@link AwsAmiTriggerCause}.
   *
   * @param trigger         trigger details
   */
  public AwsAmiTriggerCause(AwsAmiTrigger trigger) {
    super();
  }

  /**
   * Add a new filter/image match.
   *
   * @param filter   the filter that matched the image
   * @param image    the image that matched the filter
   */
  public void addMatch(AwsAmiTriggerFilter filter, Image image) {
    matches.add(new AwsAmiTriggerMatch(filter, image));
  }

  /**
   * Gets short description.
   * @return description of cause
   */
  @Override
  public String getShortDescription() {
    final List<String> imageIds = new ArrayList<String>();
    for(AwsAmiTriggerMatch match : matches) {
      imageIds.add(match.getImageId());
    }
    return Messages.Cause(StringUtils.join(imageIds, ","));
  }

  /**
   * Populates environment variables in the build environment.
   *
   * @param envVars         the environment variables to populate
   */
  public void populateEnvironment(EnvVars envVars) {
    if(!matches.isEmpty()) {
      envVars.put("awsAmiTriggerCount", String.valueOf(matches.size()));

      int num = 1;
      for(AwsAmiTriggerMatch match : matches) {
        match.populateEnvironment(envVars, String.valueOf(num++));
      }
    }
  }

  /**
   * An image and filter match.
   *
   * @author Rik Turnbull
   *
   */
  static class AwsAmiTriggerMatch {
    private final AwsAmiTriggerFilter filter;
    private final Image image;

    /**
     * Creates a new {@link AwsAmiTriggerMatch}.
     *
     * @param filter   the filter that matched the image
     * @param image    the image that matched the filter
     */
    public AwsAmiTriggerMatch(AwsAmiTriggerFilter filter, Image image) {
      this.filter = filter;
      this.image = image;
    }

    /**
     * Gets image id.
     * @return image id
     */
    public String getImageId() {
      return image.getImageId();
    }

    /**
     * Populates environment variables in the build environment.
     *
     * @param envVars   the environment variables
     * @param suffix    suffix to add to environment variable names
     */
    public void populateEnvironment(EnvVars envVars, String suffix) {
      putEnvVar(envVars, "awsAmiTriggerImageArchitecture", suffix, image.getArchitecture());
      putEnvVar(envVars, "awsAmiTriggerImageCreationDate", suffix, image.getCreationDate());
      putEnvVar(envVars, "awsAmiTriggerImageDescription", suffix, image.getDescription());
      putEnvVar(envVars, "awsAmiTriggerImageHypervisor", suffix, image.getHypervisor());
      putEnvVar(envVars, "awsAmiTriggerImageId", suffix, image.getImageId());
      putEnvVar(envVars, "awsAmiTriggerImageType", suffix, image.getImageType());
      putEnvVar(envVars, "awsAmiTriggerImageName", suffix, image.getName());
      putEnvVar(envVars, "awsAmiTriggerImageOwnerAlias", suffix, image.getImageOwnerAlias());
      putEnvVar(envVars, "awsAmiTriggerImageOwnerId", suffix, image.getOwnerId());
      putEnvVar(envVars, "awsAmiTriggerImageProductCodes", suffix, image.getProductCodes());
      putEnvVar(envVars, "awsAmiTriggerImageTags", suffix, image.getTags());
      putEnvVar(envVars, "awsAmiTriggerImageIsPublic", suffix, image.isPublic());

      putEnvVar(envVars, "awsAmiTriggerFilterArchitecture", suffix, filter.getArchitecture());
      putEnvVar(envVars, "awsAmiTriggerFilterDescription", suffix, filter.getDescription());
      putEnvVar(envVars, "awsAmiTriggerFilterName", suffix, filter.getName());
      putEnvVar(envVars, "awsAmiTriggerFilterOwnerAlias", suffix, filter.getOwnerAlias());
      putEnvVar(envVars, "awsAmiTriggerFilterOwnerId", suffix, filter.getOwnerId());
      putEnvVar(envVars, "awsAmiTriggerFilterProductCode", suffix, filter.getProductCode());
      putEnvVar(envVars, "awsAmiTriggerFilterTags", suffix, filter.getTags());
      putEnvVar(envVars, "awsAmiTriggerFilterIsPublic", suffix, filter.getShared());
    }

    /**
     * Adds a boolean value to the environment - <code>true</code>, <code>false</code> or <code>null</code>.
     *
     * @param envVars   environment variables to populate
     * @param name      name of environment variable
     * @param suffix    suffix to add to name of environment variable
     * @param value     boolean value of environment variable
     */
    private void putEnvVar(EnvVars envVars, String name, String suffix, Boolean value) {
      putEnvVar(envVars, name, suffix, BooleanUtils.toString(value, "true", "false", null));
    }

    /**
     * Adds a list value to the environment.
     *
     * @param envVars   environment variables to populate
     * @param name      name of environment variable
     * @param suffix    suffix to add to name of environment variable
     * @param value     list value of environment variable
     */
    private void putEnvVar(EnvVars envVars, String name, String suffix, List value) {
      putEnvVar(envVars, name, suffix, Objects.toString(value));
    }

    /**
     * Adds a string value to the environment (or empty value for <code>null</code>).
     *
     * @param envVars   environment variables to populate
     * @param name      name of environment variable
     * @param suffix    suffix to add to name of environment variable
     * @param value     string value of environment variable
     */
    private void putEnvVar(EnvVars envVars, String name, String suffix, String value) {
      envVars.put(name+suffix, StringUtils.defaultString(value));
    }
  }
}
