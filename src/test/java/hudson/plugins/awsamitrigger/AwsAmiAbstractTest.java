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
import com.amazonaws.services.ec2.model.HypervisorType;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.ImageTypeValues;
import com.amazonaws.services.ec2.model.ProductCode;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.util.DateUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import jenkins.model.Jenkins;

import hudson.model.BuildableItem;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

/**
 * Abstract base class for tests.
 *
 * @author Rik Turnbull
 *
 */
public abstract class AwsAmiAbstractTest {

  /**
   * Creates a new filter.
   *
   * @param architecture   image architecture (i386|x86_64)
   * @param description    description of image (provided during image creation)
   * @param name           name of ami (may be a wildcard)
   * @param ownerAlias     the AWS account alias (for example, amazon)
   * @param ownerId        the AWS account id of the image owner
   * @param productCode    the product code
   * @param tags           the key/value combination of a tag assigned to the resource
   * @param shared         aka is public
   * @return new filter
   */
  protected AwsAmiTriggerFilter createFilter(String architecture, String description,
    String name, String ownerAlias, String ownerId, String productCode, String tags, String shared) {
      return new AwsAmiTriggerFilter(architecture, description, name, ownerAlias, ownerId, productCode, tags, shared);
  }

  /**
   * Creates a new AWS image.
   *
   * @param architecture   image architecture (i386|x86_64)
   * @param description    description of image (provided during image creation)
   * @param hypervisor     hypervisor (xen|ovm)
   * @param imageId        ami id
   * @param imageType      image type (kernel|machine|ramdisk)
   * @param name           name of ami (may be a wildcard)
   * @param ownerAlias     the AWS account alias (for example, amazon)
   * @param ownerId        the AWS account id of the image owner
   * @param productCode    the product code
   * @param tagKey         a tag key
   * @param tagValue       a tag value for the tag key
   * @param shared         aka is public
   * @return a new image
   */
  protected Image createImage(String architecture, Date creationDate, String description, String hypervisor, String imageId,
    String imageType, String name, String ownerAlias, String ownerId, String productCode, String tagKey, String tagValue, String shared) {
      Image image = new Image();
      image.setArchitecture(ArchitectureValues.valueOf(architecture.toUpperCase()));
      image.setCreationDate(DateUtils.formatISO8601Date(creationDate));
      image.setDescription(description);
      image.setHypervisor(HypervisorType.valueOf(StringUtils.capitalize(hypervisor)));
      image.setImageId(imageId);
      image.setImageType(ImageTypeValues.valueOf(StringUtils.capitalize(imageType)));
      image.setName(name);
      image.setImageOwnerAlias(ownerAlias);
      image.setOwnerId(ownerId);
      image.setProductCodes(Collections.singletonList(new ProductCode().withProductCodeId(productCode)));
      image.setTags(Collections.singletonList(new Tag().withKey(tagKey).withValue(tagValue)));
      image.setPublic(shared.equals("true"));
      return image;
  }

  /**
   * Creates a trigger that will trigger depending on the image creation date.
   *
   * @param image           image that could cause the trigger to fire
   * @param spec            crontab specification that defines how often to poll
   * @param credentialsId   aws credentials id
   * @param regionName      aws region name
   * @param filters         list of filters
   * @throws ANTLRException if unable to parse the crontab specification
   * @return trigger with mocked EC2Service
   */
  protected AwsAmiTrigger createTrigger(Image image, String spec,
    String credentialsId, String regionName, List<AwsAmiTriggerFilter> filters) throws ANTLRException {
      mockEC2Service(image);
      return new AwsAmiTrigger(spec, credentialsId, regionName, filters);
  }

  /**
   * Mocks the constructor and fetchLatestImage() methods of the <code>EC2Service</code>.
   * @param creationDate creation date of latest matching image
   */
  private void mockEC2Service(Image image) {
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
  protected BuildableItem mockBuildableItem(String projectName) {
    BuildableItem buildableItemMock = PowerMockito.mock(BuildableItem.class);
    PowerMockito.when(buildableItemMock.getFullName()).thenReturn(projectName);
    return buildableItemMock;
  }
}
