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

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.util.DateUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jenkins.model.Jenkins;

import hudson.model.BuildableItem;

import org.apache.commons.lang.StringUtils;
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
 * Run tests for {@link AwsAmiTriggerFilter}.
 *
 * @author Rik Turnbull
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(value = Parameterized.class)
@PrepareForTest({EC2Service.class, Jenkins.class})
public class EC2ServiceTest extends AwsAmiAbstractTest {

  @Parameter(0)
  public int imageCount;
  @Parameter(1)
  public String image1ImageId;
  @Parameter(2)
  public String image2ImageId;
  @Parameter(3)
  public String image1CreationDate;
  @Parameter(4)
  public String image2CreationDate;
  @Parameter(5)
  public String newestImageId;

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
      new Object[][] {
        { 2, "ami-123", "ami-456", "2017-06-12T20:19:18Z", "2017-06-13T20:19:18Z", "ami-456" },
        { 2, "ami-123", "ami-456", "2017-06-14T20:19:18Z", "2017-06-13T20:19:18Z", "ami-123" }
      }
    );
  }

  @Before
  public void setUp() {
    mockAmazonEC2Client();
    mockJenkins();
  }

  /**
   * Test that the getters returns values set in the constructor.
   */
  @Test
  public void testConstructor() {
    new EC2Service(CREDENTIALS_ID, REGION_NAME);
  }

  /**
   * Tests the <code>describeImages</code> method returns images in a
   * sorted order.
   */
  @Test
  public void testDescribeImages() {
    EC2Service ec2Service = new EC2Service(CREDENTIALS_ID, REGION_NAME);
    List<Image> images = ec2Service.describeImages(null);
    Assert.assertNotNull("images", images);
    Assert.assertEquals("imageCount", imageCount, images.size());
    Assert.assertEquals("imageId", newestImageId, images.get(0).getImageId());
  }

  /**
   * Tests the <code>fetchLatestImage</code> method returns the newest image.
   */
  @Test
  public void testFetchLatestImage() {
    EC2Service ec2Service = new EC2Service(CREDENTIALS_ID, REGION_NAME);
    Image image = ec2Service.fetchLatestImage(null);
    Assert.assertNotNull("image", image);
    Assert.assertEquals("imageId", newestImageId, image.getImageId());
  }

  /**
   * Mocks the constructor and describeImages() methods of the <code>AmazonEC2Client</code>.
   */
  private void mockAmazonEC2Client() {
    AmazonEC2Client amazonEC2Client = PowerMockito.mock(AmazonEC2Client.class);
    try {
      PowerMockito.whenNew(AmazonEC2Client.class).withAnyArguments().thenReturn(amazonEC2Client);
    } catch(Exception e) {
      Assert.fail("Unexpected exception: " + e.getMessage());
    }
    PowerMockito.when(amazonEC2Client.describeImages(Mockito.any(DescribeImagesRequest.class))).thenReturn(
      new DescribeImagesResult().withImages(
        new Image[] {
          new Image().withImageId(image1ImageId).withCreationDate(image1CreationDate),
          new Image().withImageId(image2ImageId).withCreationDate(image2CreationDate)
        }
      )
    );
  }

  /**
   * Mocks the static <code>getInstance()</code> and <code>getActiveInstance</code> methods
   * of the <code>Jenkins</code> class.
   */
  private void mockJenkins() {
    Jenkins jenkins = PowerMockito.mock(Jenkins.class);
    PowerMockito.mockStatic(Jenkins.class);
    PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
    PowerMockito.when(Jenkins.getActiveInstance()).thenReturn(jenkins);
  }
}
