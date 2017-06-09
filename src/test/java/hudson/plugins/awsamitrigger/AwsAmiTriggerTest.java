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
import org.mockito.Mockito;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.easymock.annotation.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Run tests for {@link AwsAmiTrigger}.
 *
 * @author Rik Turnbull
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AwsAmiTrigger.class})
public class AwsAmiTriggerTest {

  private final static String PROJECT_NAME = "build-me";

  private final static String CREDENTIALS_ID = "aws-credentials";
  private final static String CREDENTIALS_ID_PATTERN = "aws\\-credentials";
  private final static String REGION_NAME = "eu-west-1";
  private final static String REGION_NAME_PATTERN = "eu\\-west\\-1";
  private final static String SPEC = "* * * * *";

  private final static String IMAGE_ID = "ami-abc123";

  private final static String ARCHITECTURE = "x86_64";
  private final static String DESCRIPTION = "description";
  private final static String NAME = "name";
  private final static String OWNER_ALIAS = "ownerAlias";
  private final static String OWNER_ID = "ownerId";
  private final static String PRODUCT_CODE = "productCode";
  private final static String TAG_KEY = "project";
  private final static String TAG_VALUE = "jenkins";
  private final static String TAGS = TAG_KEY + "=" + TAG_VALUE;
  private final static String SHARED = "true";

  private final static String FILTER_TO_STRING =
      "architecture=" + ARCHITECTURE + "," +
      "description="  + DESCRIPTION  + "," +
      "name="         + NAME         + "," +
      "ownerAlias="   + OWNER_ALIAS  + "," +
      "ownerId="      + OWNER_ID     + "," +
      "productCode="  + PRODUCT_CODE + "," +
      "tags="         + TAGS         + "," +
      "shared="       + SHARED;

  /**
   * Tests that the getters returns values set in the constructor.
   * @throws ANTLRException if there is a problem with the trigger spec
   */
  @Test
  public void testConstructor() throws ANTLRException {
    AwsAmiTrigger trigger = getTrigger();
    Assert.assertEquals("getCredentialsId()", trigger.getCredentialsId(), CREDENTIALS_ID);
    Assert.assertEquals("getRegionName()", trigger.getRegionName(), REGION_NAME);
    Assert.assertEquals("getSpec()", trigger.getSpec(), SPEC);
    Assert.assertEquals("getFilters()", trigger.getFilters().toString(), "[AwsAmiTriggerFilter[" + FILTER_TO_STRING + "]]");
    Assert.assertTrue("getLastRun()", ((new Date().getTime()-trigger.getLastRun().getTime())<1000));
  }

  /**
   * Tests the <code>toString()</code> method returns all the fields.
   * @throws ANTLRException if there is a problem with the trigger spec
   */
  @Test
  public void testToString() throws ANTLRException {
    Assert.assertThat(getTrigger().toString(), Matchers.matchesPattern("AwsAmiTrigger\\[" +
      "credentialsId=" + CREDENTIALS_ID_PATTERN  + "," +
      "regionName="    + REGION_NAME_PATTERN     + "," +
      "lastRun="       + ".{28}"                 + "," +
      "filters="       +
        "\\[AwsAmiTriggerFilter\\[" +
              FILTER_TO_STRING +
         "\\]\\]"              +
      "\\]"
    ));
  }

  /**
   * Tests that a build is triggered when a new image is found.
   * @throws ANTLRException if there is a problem with the trigger spec
   */
  @Test
  public void testRunAndSchedule() throws ANTLRException {
    runTriggerAndCountMethodCalls(getTrigger(new Date(System.currentTimeMillis()*(1000*60*30))), 1, 1);
  }

  /**
   * Tests that a build is NOT triggered when a new image is not found.
   * @throws ANTLRException if there is a problem with the trigger spec
   */
   @Test
  public void testRunAndDoNotSchedule() throws ANTLRException {
    runTriggerAndCountMethodCalls(getTrigger(), 0, 0);
  }

  /**
    * Runs the trigger and counts the number of method calls.
    * @param trigger                  the trigger to run
    * @param saveMethodCount          number of save() method calls
    * @param scheduleBuildMethodCount number of scheduleBuild() method calls
    */
  private void runTriggerAndCountMethodCalls(AwsAmiTrigger trigger, int saveMethodCount, int scheduleBuildMethodCount) {
    try {
      BuildableItem buildableItemMock = mockBuildableItem();
      trigger.start(buildableItemMock, true);
      trigger.run();
      Mockito.verify(buildableItemMock, Mockito.times(saveMethodCount)).save();
      Mockito.verify(buildableItemMock, Mockito.times(scheduleBuildMethodCount)).scheduleBuild(Mockito.any(AwsAmiTriggerCause.class));
    } catch(IOException e) {
      Assert.fail("Unexpected exception: " + e.getMessage());
    }
  }

  /**
   * Returns a trigger that will not trigger.
   * @return trigger with mocked EC2Service
   * @throws ANTLRException if there is a problem with the trigger spec
   */
  private AwsAmiTrigger getTrigger() throws ANTLRException {
      return getTrigger(new Date());
  }

  /**
   * Returns a trigger that will trigger depending on the image creation date.
   * @param creationDate creation date of latest matching image
   * @return trigger with mocked EC2Service
   * @throws ANTLRException if there is a problem with the trigger spec
   */
  private AwsAmiTrigger getTrigger(Date creationDate) throws ANTLRException {
    mockEC2Service(creationDate);
    return new AwsAmiTrigger(SPEC, CREDENTIALS_ID, REGION_NAME, Collections.singletonList(getFilter()));
  }

  /**
   * Gets a filter.
   * @return a filter
   */
  private AwsAmiTriggerFilter getFilter() {
    return new AwsAmiTriggerFilter(ARCHITECTURE, DESCRIPTION, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED);
  }

  /**
   * Mocks the constructor and fetchLatestImage() methods of the <code>EC2Service</code>.
   * @param creationDate creation date of latest matching image
   */
  private static void mockEC2Service(Date creationDate) {
    Image image = new Image();
    image.setArchitecture(ArchitectureValues.valueOf(ARCHITECTURE.toUpperCase()));
    image.setCreationDate(DateUtils.formatISO8601Date(creationDate));
    image.setDescription(DESCRIPTION);
    image.setImageId(IMAGE_ID);
    image.setName(NAME);
    image.setImageOwnerAlias(OWNER_ALIAS);
    image.setOwnerId(OWNER_ID);
    image.setProductCodes(Collections.singletonList(new ProductCode().withProductCodeId(PRODUCT_CODE)));
    image.setTags(Collections.singletonList(new Tag().withKey(TAG_KEY).withValue(TAG_VALUE)));
    image.setPublic(SHARED.equals("true"));

    EC2Service ec2ServiceMock = PowerMockito.mock(EC2Service.class);
    PowerMockito.when(ec2ServiceMock.fetchLatestImage(Mockito.any(Collection.class))).thenReturn(image);
    try {
      PowerMockito.whenNew(EC2Service.class).withAnyArguments().thenReturn(ec2ServiceMock);
    } catch(Exception e) {
      Assert.fail("Unexpected exception: " + e.getMessage());
    }
  }

  /**
   * Mocks a the <code>getFullName()</code> method of the <code>BuildableItem</code>
   * class. This method must return a value so that the trigger can start.
   * @return BuildableItem mocked BuildableItem
   */
  private static BuildableItem mockBuildableItem() {
    BuildableItem buildableItemMock = PowerMockito.mock(BuildableItem.class);
    PowerMockito.when(buildableItemMock.getFullName()).thenReturn(PROJECT_NAME);
    return buildableItemMock;
  }
}
