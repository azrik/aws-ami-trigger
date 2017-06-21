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

import antlr.ANTLRException;

import com.amazonaws.services.ec2.model.ArchitectureValues;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.ProductCode;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.util.DateUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import jenkins.model.Jenkins;

import hudson.model.BuildableItem;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.easymock.annotation.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

/**
 * Run tests for {@link AwsAmiTrigger}.
 *
 * @author Rik Turnbull
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest(AwsAmiTrigger.class)
public class AwsAmiTriggerTest extends AwsAmiAbstractTest {

  @Parameter(0)
  public String credentialsId;
  @Parameter(1)
  public String regionName;
  @Parameter(2)
  public String spec;
  @Parameter(3)
  public String filterArchitecture;
  @Parameter(4)
  public String filterDescription;
  @Parameter(5)
  public String filterName;
  @Parameter(6)
  public String filterOwnerAlias;
  @Parameter(7)
  public String filterOwnerId;
  @Parameter(8)
  public String filterProductCode;
  @Parameter(9)
  public String filterTags;
  @Parameter(10)
  public String filterShared;
  @Parameter(11)
  public String imageArchitecture;
  @Parameter(12)
  public Date imageCreationDate;
  @Parameter(13)
  public String imageDescription;
  @Parameter(14)
  public String imageName;
  @Parameter(15)
  public String imageOwnerAlias;
  @Parameter(16)
  public String imageOwnerId;
  @Parameter(17)
  public String imageProductCode;
  @Parameter(18)
  public String imageTags;
  @Parameter(19)
  public String imageShared;
  @Parameter(20)
  public String imageHypervisor;
  @Parameter(21)
  public String imageId;
  @Parameter(22)
  public String imageType;
  @Parameter(23)
  public String imageTagKey;
  @Parameter(24)
  public String imageTagValue;
  @Parameter(25)
  public int saveMethodCount;
  @Parameter(26)
  public int scheduleBuildMethodCount;
  @Parameter(27)
  public Boolean expectANTLRException;

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
      new Object[][] {
        { CREDENTIALS_ID, REGION_NAME, SPEC,
            ARCHITECTURE, DESCRIPTION, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED,
            ARCHITECTURE, new Date(System.currentTimeMillis()+(1000*60*30)), DESCRIPTION, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED,
            HYPERVISOR, IMAGE_ID, IMAGE_TYPE, TAG_KEY, TAG_VALUE, 1, 1, false },
        { CREDENTIALS_ID, REGION_NAME, SPEC,
            ARCHITECTURE, DESCRIPTION, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED,
            ARCHITECTURE, new Date(), DESCRIPTION, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED,
            HYPERVISOR, IMAGE_ID, IMAGE_TYPE, TAG_KEY, TAG_VALUE, 0, 0, false },
        { CREDENTIALS_ID, REGION_NAME, "invalid-spec",
            ARCHITECTURE, DESCRIPTION, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED,
            ARCHITECTURE, new Date(), DESCRIPTION, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED,
            HYPERVISOR, IMAGE_ID, IMAGE_TYPE, TAG_KEY, TAG_VALUE, 0, 0, true },
      }
    );
  }

  /**
   * Test that the getters returns values set in the constructor.
   * @throws ANTLRException if there is a problem with the trigger spec
   */
  @Test
  public void testConstructor() {
    try {
      AwsAmiTrigger trigger = createTrigger();
      Assert.assertEquals("getCredentialsId()", credentialsId, trigger.getCredentialsId());
      Assert.assertEquals("getRegionName()", regionName, trigger.getRegionName());
      Assert.assertEquals("getSpec()", spec, trigger.getSpec());
      Assert.assertEquals("getFilters()", "[AwsAmiTriggerFilter[" +
          "architecture=" + nullIfEmpty(filterArchitecture) + "," +
          "description="  + nullIfEmpty(filterDescription)  + "," +
          "name="         + nullIfEmpty(filterName)         + "," +
          "ownerAlias="   + nullIfEmpty(filterOwnerAlias)   + "," +
          "ownerId="      + nullIfEmpty(filterOwnerId)      + "," +
          "productCode="  + nullIfEmpty(filterProductCode)  + "," +
          "tags="         + nullIfEmpty(filterTags)         + "," +
          "shared="       + nullIfEmpty(filterShared)             +
        "]]", trigger.getFilters().toString()
      );
      Assert.assertTrue("getLastRun()", ((new Date().getTime()-trigger.getLastRun().getTime())<1000));
    } catch(ANTLRException e) {
      Assert.assertTrue("ANTLRException", expectANTLRException);
    }
  }

  /**
   * Test the <code>toString()</code> method returns all the fields.
   */
  @Test
  public void testToString() {
    try {
      Assert.assertThat(createTrigger().toString(), Matchers.matchesPattern(escapeString("AwsAmiTrigger[" +
        "credentialsId=" + nullIfEmpty(credentialsId)  + "," +
        "regionName="    + nullIfEmpty(regionName)     + "," +
        "lastRun="       + ".{28}"                     + "," +
        "filters="       +
          "[AwsAmiTriggerFilter[" +
            "architecture=" + nullIfEmpty(filterArchitecture) + "," +
            "description="  + nullIfEmpty(filterDescription)  + "," +
            "name="         + nullIfEmpty(filterName)         + "," +
            "ownerAlias="   + nullIfEmpty(filterOwnerAlias)   + "," +
            "ownerId="      + nullIfEmpty(filterOwnerId)      + "," +
            "productCode="  + nullIfEmpty(filterProductCode)  + "," +
            "tags="         + nullIfEmpty(filterTags)         + "," +
            "shared="       + nullIfEmpty(filterShared)             +
           "]]"             +
        "]"
      )));
    } catch(ANTLRException e) {
      Assert.assertTrue("ANTLRException", expectANTLRException);
    }
  }

  /**
    * Runs the trigger and counts the number of method calls.
    */
  private void testRunTrigger() {
    try {
      AwsAmiTrigger trigger = createTrigger();
      BuildableItem buildableItemMock = mockBuildableItem();
      trigger.start(buildableItemMock, true);
      trigger.run();
      Mockito.verify(buildableItemMock, Mockito.times(saveMethodCount)).save();
      Mockito.verify(buildableItemMock, Mockito.times(scheduleBuildMethodCount)).scheduleBuild(Mockito.any(AwsAmiTriggerCause.class));
    } catch(ANTLRException ae) {
      Assert.assertTrue("ANTLRException", expectANTLRException);
    } catch(IOException ioe) {
      Assert.fail("Unexpected exception: " + ioe.getMessage());
    }
  }

  /**
   * Creates a new trigger.
   *
   * @return a trigger
   * @throws ANTLRException if there is a problem with the trigger spec
   **/
  private AwsAmiTrigger createTrigger() throws ANTLRException {
    mockEC2Service();
    return createTrigger(spec, credentialsId, regionName,
      Collections.singletonList(createFilter(filterArchitecture, filterDescription, filterName, filterOwnerAlias,
        filterOwnerId, filterProductCode, filterTags, filterShared)));
  }

  /**
   * Mocks a the <code>getFullName()</code> method of the <code>BuildableItem</code>
   * class. This method must return a value so that the trigger can start.
   *
   * @return BuildableItem mocked BuildableItem
   */
  private BuildableItem mockBuildableItem() {
    BuildableItem buildableItemMock = PowerMockito.mock(BuildableItem.class);
    PowerMockito.when(buildableItemMock.getFullName()).thenReturn("projectName");
    return buildableItemMock;
  }

  /**
   * Mocks the constructor and fetchLatestImage() methods of the <code>EC2Service</code>.
   */
  private void mockEC2Service() {
    EC2Service ec2ServiceMock = PowerMockito.mock(EC2Service.class);
    PowerMockito.when(ec2ServiceMock.fetchLatestImage(Mockito.any(Collection.class))).thenReturn(
      createImage(imageArchitecture, imageCreationDate, imageDescription, imageHypervisor, imageId,
        imageType, imageName, imageOwnerAlias, imageOwnerId, imageProductCode, imageTagKey, imageTagValue, imageShared));
    try {
      PowerMockito.whenNew(EC2Service.class).withAnyArguments().thenReturn(ec2ServiceMock);
    } catch(Exception e) {
      Assert.fail("Unexpected exception: " + e.getMessage());
    }
  }
}
