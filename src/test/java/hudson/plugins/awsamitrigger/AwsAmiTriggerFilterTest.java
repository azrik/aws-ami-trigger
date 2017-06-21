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

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.util.DateUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

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
public class AwsAmiTriggerFilterTest extends AwsAmiAbstractTest {

  @Parameter(0)
  public String architecture;
  @Parameter(1)
  public String description;
  @Parameter(2)
  public String name;
  @Parameter(3)
  public String ownerAlias;
  @Parameter(4)
  public String ownerId;
  @Parameter(5)
  public String productCode;
  @Parameter(6)
  public String tags;
  @Parameter(7)
  public String shared;
  @Parameter(8)
  public String expectedTags[];

  /**
   * Initializes test data.
   *
   * @return a collection of test data to populate the parameter fields
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(
      new Object[][] {
        { ARCHITECTURE, DESCRIPTION, NAME, OWNER_ALIAS, OWNER_ID, PRODUCT_CODE, TAGS, SHARED, new String[]{TAG_KEY, TAG_VALUE} },
        { ANY, DESCRIPTION, NAME, ANY, OWNER_ID, PRODUCT_CODE, TAGS, ANY, new String[]{TAG_KEY, TAG_VALUE} },
        { null, null, null, null, null, null, null, null, null },
        { "", "", "", "", "", "", "Project=jenkins;Owner=hudson", "", new String[]{"Project", "jenkins", "Owner", "hudson"} }
      }
    );
  }

  /**
   * Test that the getters returns values set in the constructor.
   */
  @Test
  public void testConstructor() {
    AwsAmiTriggerFilter filter = createFilter();
    Assert.assertEquals("getArchitecture()", architecture, filter.getArchitecture());
    Assert.assertEquals("getDescription()", description, filter.getDescription());
    Assert.assertEquals("getName()", name, filter.getName());
    Assert.assertEquals("getOwnerAlias()", ownerAlias, filter.getOwnerAlias());
    Assert.assertEquals("getOwnerId()", ownerId, filter.getOwnerId());
    Assert.assertEquals("getProductCode()", productCode, filter.getProductCode());
    Assert.assertEquals("getTags()", tags, filter.getTags());
    Assert.assertEquals("getShared()", shared, filter.getShared());
  }

  /**
   * Test that the filter returns propert AWS filters.
   */
  @Test
  public void testToAWSFilters() {
    Collection<Filter> filters = createFilter().toAWSFilters();
    assertFilterHasValue(filters, "architecture", architecture);
    assertFilterHasValue(filters, "description", description);
    assertFilterHasValue(filters, "description", description);
    assertFilterHasValue(filters, "name", name);
    assertFilterHasValue(filters, "owner-alias", ownerAlias);
    assertFilterHasValue(filters, "owner-id", ownerId);
    assertFilterHasValue(filters, "product-code", productCode);
    assertFilterHasValue(filters, "is-public", shared);
    assertFilterHasValue(filters, "state", "available");
    if(expectedTags != null) {
      for(int i = 0; i < expectedTags.length; i+=2) {
        assertFilterHasValue(filters, "tag:"+expectedTags[i], expectedTags[i+1]);
      }
    }
  }

  /**
   * Test the <code>toString()</code> method returns all the fields.
   */
  @Test
  public void testToString() {
    Assert.assertEquals("AwsAmiTriggerFilter[" +
        "architecture=" + nullIfEmpty(architecture) + "," +
        "description="  + nullIfEmpty(description)  + "," +
        "name="         + nullIfEmpty(name)         + "," +
        "ownerAlias="   + nullIfEmpty(ownerAlias)   + "," +
        "ownerId="      + nullIfEmpty(ownerId)      + "," +
        "productCode="  + nullIfEmpty(productCode)  + "," +
        "tags="         + nullIfEmpty(tags)         + "," +
        "shared="       + nullIfEmpty(shared)             +
      "]", createFilter().toString()
    );
  }

  /**
   * Asserts that a filter exists with the given <code>filterName</code> and
   * has the <code>expectedValue</code>.
   *
   * @param filters a collection of filters to validates
   * @param filterName the name of the filter to validate
   * @param expectedValue the expected value of the filter
   */
  private void assertFilterHasValue(Collection<Filter> filters, String filterName, String expectedValue) {
    Filter filter = findFilterByName(filters, filterName);
    if(StringUtils.isEmpty(expectedValue) || ANY.equals(expectedValue)) {
      Assert.assertNull(filterName, filter);
    } else {
      Assert.assertNotNull(filterName, filter);
      Assert.assertNotNull(filterName, filter.getValues());
      Assert.assertEquals(filterName, 1, filter.getValues().size());
      Assert.assertEquals(filterName, expectedValue, filter.getValues().get(0));
    }
  }

  /**
   * Finds a filter by name in a collection of filters.
   *
   * @param filters a collection of filters to search
   * @paramter filterName the name of the filter to find
   * @return the filter or null if it is not found
   */
  private Filter findFilterByName(Collection<Filter> filters, String filterName) {
    for(Filter filter : filters) {
      if(filterName.equals(filter.getName())) {
        return filter;
      }
    }
    return null;
  }

  /**
   * Creates a new filter.
   *
   * @return a filter
   **/
  private AwsAmiTriggerFilter createFilter() {
    return createFilter(architecture, description, name, ownerAlias, ownerId, productCode, tags, shared);
  }
}
