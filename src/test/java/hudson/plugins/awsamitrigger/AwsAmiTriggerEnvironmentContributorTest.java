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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import hudson.EnvVars;
import hudson.model.Run;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

/**
 * Run tests for {@link AwsAmiTriggerEnvironmentContributor}.
 *
 * @author Rik Turnbull
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
public class AwsAmiTriggerEnvironmentContributorTest extends AwsAmiAbstractTest {

  @Parameter(0)
  public String filterArchitecture;
  @Parameter(1)
  public String filterDescription;
  @Parameter(2)
  public String filterName;
  @Parameter(3)
  public String filterOwnerAlias;
  @Parameter(4)
  public String filterOwnerId;
  @Parameter(5)
  public String filterProductCode;
  @Parameter(6)
  public String filterTags;
  @Parameter(7)
  public String filterShared;
  @Parameter(8)
  public String imageArchitecture;
  @Parameter(9)
  public Date imageCreationDate;
  @Parameter(10)
  public String imageDescription;
  @Parameter(11)
  public String imageName;
  @Parameter(12)
  public String imageOwnerAlias;
  @Parameter(13)
  public String imageOwnerId;
  @Parameter(14)
  public String imageProductCode;
  @Parameter(15)
  public String imageTags;
  @Parameter(16)
  public String imageShared;
  @Parameter(17)
  public String imageHypervisor;
  @Parameter(18)
  public String imageId;
  @Parameter(19)
  public String imageType;
  @Parameter(20)
  public String imageTagKey;
  @Parameter(21)
  public String imageTagValue;
  @Parameter(22)
  public int testMatches;
  @Parameter(23)
  public String expectedImageDescription;

  /**
   * Initializes test data.
   *
   * @return a collection of test data to populate the parameter fields
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
      new Object[][] {
        {
          /* filter */
          ARCHITECTURE, DESCRIPTION, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED,
           /* image */
          ARCHITECTURE, new Date(System.currentTimeMillis()+(1000*60*30)), DESCRIPTION, NAME,
          OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED, HYPERVISOR, IMAGE_ID, IMAGE_TYPE,
          TAG_KEY, TAG_VALUE,
          /* test params */
          0,
          /* expected values */
          DESCRIPTION
        },
        {
          /* filter */
          ARCHITECTURE, DESCRIPTION, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED,
          /* image */
          ARCHITECTURE, new Date(), DESCRIPTION, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE,
          TAGS, SHARED, HYPERVISOR, IMAGE_ID, IMAGE_TYPE, TAG_KEY, TAG_VALUE,
          /* test params */
          1,
          /* expected values */
          DESCRIPTION
        },
        {
          /* filter */
          ARCHITECTURE, DESCRIPTION, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED,
          /* image */
          ARCHITECTURE, new Date(), null, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED,
          HYPERVISOR, IMAGE_ID, IMAGE_TYPE, TAG_KEY, TAG_VALUE,
          /* test params */
          2,
          /* expected values */
          ""
        },
      }
    );
  }

  /**
   * Tests that the environment variables are populated correctly
   * for a certain number of matches.
   *
   */
  @Test
  public void testPopulateEnvironment() {
    AwsAmiTriggerEnvironmentContributor environmentContributor = new AwsAmiTriggerEnvironmentContributor();

    EnvVars envVars = new EnvVars();
    environmentContributor.buildEnvironmentFor(mockRun(), envVars, null);
    for(int i = 0; i < testMatches; i++) {
      assertEnvironmentVariablesSet(envVars, i+1);
    }
  }

  /**
   * Asserts that the constants have been added to the environment.
   *
   * @param envVars  environment variables
   * @param suffix   suffix for environment variable names
   */
  private void assertEnvironmentVariablesSet(EnvVars envVars, int suffix) {
    Assert.assertEquals("envImageArchitecture", imageArchitecture, envVars.get("awsAmiTriggerImageArchitecture" + suffix));
    Assert.assertEquals("envImageCreationDate", DateUtils.formatISO8601Date(imageCreationDate), envVars.get("awsAmiTriggerImageCreationDate" + suffix));
    Assert.assertEquals("envImageDescription", expectedImageDescription, envVars.get("awsAmiTriggerImageDescription" + suffix));
    Assert.assertEquals("envImageHypervisor", imageHypervisor, envVars.get("awsAmiTriggerImageHypervisor" + suffix));
    Assert.assertEquals("envImageId", imageId, envVars.get("awsAmiTriggerImageId" + suffix));
    Assert.assertEquals("envImageType", imageType, envVars.get("awsAmiTriggerImageType" + suffix));
    Assert.assertEquals("envImageName", imageName, envVars.get("awsAmiTriggerImageName" + suffix));
    Assert.assertEquals("envImageOwnerAlias", imageOwnerAlias, envVars.get("awsAmiTriggerImageOwnerAlias" + suffix));
    Assert.assertEquals("envImageOwnerId", imageOwnerId, envVars.get("awsAmiTriggerImageOwnerId" + suffix));
    Assert.assertEquals("envImageProductCodes", imageProductCode, envVars.get("awsAmiTriggerImageProductCodes" + suffix));
    Assert.assertEquals("envImageTags", imageTags, envVars.get("awsAmiTriggerImageTags" + suffix));
    Assert.assertEquals("envImageIsPublic", imageShared, envVars.get("awsAmiTriggerImageIsPublic" + suffix));

    Assert.assertEquals("envFilterArchitecture", filterArchitecture, envVars.get("awsAmiTriggerFilterArchitecture" + suffix));
    Assert.assertEquals("envFilterDescription", filterDescription, envVars.get("awsAmiTriggerFilterDescription" + suffix));
    Assert.assertEquals("envFilterName", filterName, envVars.get("awsAmiTriggerFilterName" + suffix));
    Assert.assertEquals("envFilterOwnerAlias", filterOwnerAlias, envVars.get("awsAmiTriggerFilterOwnerAlias" + suffix));
    Assert.assertEquals("envFilterOwnerId", filterOwnerId, envVars.get("awsAmiTriggerFilterOwnerId" + suffix));
    Assert.assertEquals("envFilterProductCode", filterProductCode, envVars.get("awsAmiTriggerFilterProductCode" + suffix));
    Assert.assertEquals("envFilterTags", filterTags, envVars.get("awsAmiTriggerFilterTags" + suffix));
    Assert.assertEquals("envFilterIsPublic", filterShared, envVars.get("awsAmiTriggerFilterIsPublic" + suffix));
  }

  /**
   * Gets a cause with the number of given <code>testMatches</code>.
   *
   * @return a trigger cause
   */
  private AwsAmiTriggerCause createCause() {
    AwsAmiTriggerCause cause = new AwsAmiTriggerCause();
    for(int i = 0; i < testMatches; i++) {
      cause.addMatch(createFilter(), createImage());
    }
    return cause;
  }

  /**
   * Creates an image based on the test values.
   *
   * @return an image
   */
  private Image createImage() {
    return createImage(imageArchitecture, imageCreationDate, imageDescription, imageHypervisor, imageId,
      imageType, imageName, imageOwnerAlias, imageOwnerId, imageProductCode, imageTagKey, imageTagValue, imageShared);
  }

  /**
   * Creates a filter based on the test values.
   *
   * @return a filter
   */
  private AwsAmiTriggerFilter createFilter() {
    return createFilter(filterArchitecture, filterDescription, filterName,
      filterOwnerAlias, filterOwnerId, filterProductCode, filterTags, filterShared);
  }

  /**
   * Mocks a the <code>getCause()</code> method of the <code>Run</code>
   * class.
   *
   * @return BuildableItem mocked BuildableItem
   */
  private Run mockRun() {
    Run run = PowerMockito.mock(Run.class);
    PowerMockito.when(run.getCause(Mockito.any(Class.class))).thenReturn(createCause());
    return run;
  }
}
