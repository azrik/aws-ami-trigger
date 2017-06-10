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
import com.amazonaws.services.ec2.model.ProductCode;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.util.DateUtils;

import java.util.Date;

import hudson.EnvVars;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Run tests for {@link AwsAmiTrigger}.
 *
 * @author Rik Turnbull
 *
 */
@RunWith(PowerMockRunner.class)
public class AwsAmiTriggerCauseTest extends AwsAmiAbstractTest {

  private final static String ARCHITECTURE = "x86_64";
  private final static Date CREATION_DATE = new Date();
  private final static String DESCRIPTION = "description";
  private final static String HYPERVISOR = "xen";
  private final static String IMAGE_ID = "ami-abc123";
  private final static String IMAGE_TYPE = "machine";
  private final static String NAME = "name";
  private final static String OWNER_ALIAS = "ownerAlias";
  private final static String OWNER_ID = "ownerId";
  private final static String PRODUCT_CODE = "productCode";
  private final static String TAG_KEY = "project";
  private final static String TAG_VALUE = "jenkins";
  private final static String TAGS = TAG_KEY + "=" + TAG_VALUE;
  private final static String SHARED = "true";

  /**
   * Test that the short description is correct.
   */
  @Test
  public void testAddMatch() {
    AwsAmiTriggerCause cause = new AwsAmiTriggerCause();
    AwsAmiTriggerFilter filter = getFilter();
    Image image = getImage();
    cause.addMatch(filter, image);

    Assert.assertEquals("getCredentialsId()", "Started due to new matching image(s): " + IMAGE_ID, cause.getShortDescription());
  }

  /**
   * Test that nothing fails if no filters match.
   */
  @Test
  public void testPopulateEnvironmentNone() {
    AwsAmiTriggerCause cause = new AwsAmiTriggerCause();
    AwsAmiTriggerFilter filter = getFilter();
    Image image = getImage();
    cause.addMatch(filter, image);

    EnvVars envVars = new EnvVars();
    cause.populateEnvironment(envVars);
  }

  /**
   * Test that the environment variables are populated correctly when
   * only one filter matches.
   */
  @Test
  public void testPopulateEnvironmentSingle() {
    AwsAmiTriggerCause cause = new AwsAmiTriggerCause();
    AwsAmiTriggerFilter filter = getFilter();
    Image image = getImage();
    cause.addMatch(filter, image);

    EnvVars envVars = new EnvVars();
    cause.populateEnvironment(envVars);

    assertEnvironmentVariablesSet(envVars,1);
  }

  /**
   * Test that the environment variables are populated correctly when
   * multiple filters match.
   */
  @Test
  public void testPopulateEnvironmentMultiple() {
    AwsAmiTriggerCause cause = new AwsAmiTriggerCause();
    AwsAmiTriggerFilter filter = getFilter();
    Image image = getImage();
    cause.addMatch(filter, image);
    cause.addMatch(filter, image);

    EnvVars envVars = new EnvVars();
    cause.populateEnvironment(envVars);

    for(int suffix = 1; suffix <=2; suffix ++) {
      assertEnvironmentVariablesSet(envVars, suffix);
    }
  }

  /**
   * Asserts that the constants have been added to the environment.
   *
   * @param envVars  environment variables
   * @param suffix   suffix for environment variable names
   */
  private void assertEnvironmentVariablesSet(EnvVars envVars, int suffix) {
    Assert.assertEquals("envImageArchitecture", ARCHITECTURE, envVars.get("awsAmiTriggerImageArchitecture" + suffix));
    Assert.assertEquals("envImageCreationDate", DateUtils.formatISO8601Date(CREATION_DATE), envVars.get("awsAmiTriggerImageCreationDate" + suffix));
    Assert.assertEquals("envImageDescription", DESCRIPTION, envVars.get("awsAmiTriggerImageDescription" + suffix));
    Assert.assertEquals("envImageHypervisor", HYPERVISOR, envVars.get("awsAmiTriggerImageHypervisor" + suffix));
    Assert.assertEquals("envImageId", IMAGE_ID, envVars.get("awsAmiTriggerImageId" + suffix));
    Assert.assertEquals("envImageType", IMAGE_TYPE, envVars.get("awsAmiTriggerImageType" + suffix));
    Assert.assertEquals("envImageName", NAME, envVars.get("awsAmiTriggerImageName" + suffix));
    Assert.assertEquals("envImageOwnerAlias", OWNER_ALIAS, envVars.get("awsAmiTriggerImageOwnerAlias" + suffix));
    Assert.assertEquals("envImageOwnerId", OWNER_ID, envVars.get("awsAmiTriggerImageOwnerId" + suffix));
    Assert.assertEquals("envImageProductCodes", PRODUCT_CODE, envVars.get("awsAmiTriggerImageProductCodes" + suffix));
    Assert.assertEquals("envImageTags", TAGS, envVars.get("awsAmiTriggerImageTags" + suffix));
    Assert.assertEquals("envImageIsPublic", SHARED, envVars.get("awsAmiTriggerImageIsPublic" + suffix));

    Assert.assertEquals("envFilterArchitecture", ARCHITECTURE, envVars.get("awsAmiTriggerFilterArchitecture" + suffix));
    Assert.assertEquals("envFilterDescription", DESCRIPTION, envVars.get("awsAmiTriggerFilterDescription" + suffix));
    Assert.assertEquals("envFilterName", NAME, envVars.get("awsAmiTriggerFilterName" + suffix));
    Assert.assertEquals("envFilterOwnerAlias", OWNER_ALIAS, envVars.get("awsAmiTriggerFilterOwnerAlias" + suffix));
    Assert.assertEquals("envFilterOwnerId", OWNER_ID, envVars.get("awsAmiTriggerFilterOwnerId" + suffix));
    Assert.assertEquals("envFilterProductCode", PRODUCT_CODE, envVars.get("awsAmiTriggerFilterProductCode" + suffix));
    Assert.assertEquals("envFilterTags", TAGS, envVars.get("awsAmiTriggerFilterTags" + suffix));
    Assert.assertEquals("envFilterIsPublic", SHARED, envVars.get("awsAmiTriggerFilterIsPublic" + suffix));
  }

  /**
   * Gets a filter based on the constant test values.
   * @return a filter
   */
  private AwsAmiTriggerFilter getFilter() {
    return createFilter(ARCHITECTURE, DESCRIPTION,
      NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED);
  }

  /**
   * Gets an image based on the constant values.
   * @return an image
   */
  private Image getImage() {
    return createImage(ARCHITECTURE, CREATION_DATE, DESCRIPTION, HYPERVISOR,
      IMAGE_ID, IMAGE_TYPE, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAG_KEY, TAG_VALUE, SHARED);
  }
}
