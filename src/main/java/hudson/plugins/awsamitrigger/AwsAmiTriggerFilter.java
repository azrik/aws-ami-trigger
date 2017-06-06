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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.services.ec2.model.Filter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import jenkins.model.Jenkins;

/**
 * A search filter for AWS AMIs.
 *
 * @author Rik Turnbull
 *
 */
public final class AwsAmiTriggerFilter extends AbstractDescribableImpl<AwsAmiTriggerFilter> {
  public static final String ANY = "- any -";

  private static final Logger LOGGER = Logger.getLogger(AwsAmiTriggerFilter.class.getName());

  private final String architecture;
  private final String description;
  private final String name;
  private final String ownerAlias;
  private final String ownerId;
  private final String productCode;
  private final String tags;
  private final String shared;

  /**
   * Creates a new {@link AwsAmiTriggerFilter}.
   *
   * @param architecture   image architecture (i386|x86_64)
   * @param description    description of image (provided during image creation)
   * @param name           name of ami (may be a wildcard)
   * @param ownerAlias     the AWS account alias (for example, amazon)
   * @param ownerId        the AWS account id of the image owner
   * @param productCode    the product code
   * @param tags           the key/value combination of a tag assigned to the resource
   * @param shared         aka is public
   */
  @DataBoundConstructor
  public AwsAmiTriggerFilter(String architecture, String description, String name, String ownerAlias, String ownerId, String productCode, String tags, String shared) {
    this.architecture = architecture;
    this.description = description;
    this.name = name;
    this.ownerAlias = ownerAlias;
    this.ownerId = ownerId;
    this.productCode = productCode;
    this.tags = tags;
    this.shared = shared;
  }

  /**
   * Gets architecture filter.
   * @return image architecture (i386|x86_64)
   */
  public String getArchitecture() {
    return architecture;
  }

  /**
   * Gets description filter.
   * @return description of image (may be wildcarded)
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets name filter.
   * @return name of image (may be wildcarded)
   */
  public String getName() {
    return name;
  }

  /**
   * Gets owner alias filter.
   * @return owner alias for image
   */
  public String getOwnerAlias() {
    return ownerAlias;
  }

  /**
   * Gets owner id filter.
   * @return owner id for image
   */
  public String getOwnerId() {
    return ownerId;
  }

  /**
   * Gets product code filter.
   * @return product code for image
   */
  public String getProductCode() {
    return productCode;
  }

  /**
   * Gets tags filter.
   * @return tags in format key=value;key=value
   */
  public String getTags() {
    return tags;
  }

  /**
   * Gets is-public filter.
   * @return is-public value for image (true|false)
   */
  public String getShared() {
    return shared;
  }

  /**
   * Converts {@link AwsAmiTriggerFilter} into a Collection of
   * AWS spec <code>Filter</code> objects.
   *
   * <p>Any filters that are empty are ignored or set to -any- are
   * ignored. A filter of <code>state=available</code> is always added.
   * Tags are parsed and added as individual <code>Filter</code> objects.</p>
   *
   * @return Collection of Filter objects
   */
  public Collection<Filter> toAWSFilters() {
    Collection<Filter> filters = new ArrayList<Filter>();
    filters.add(new Filter("state", Collections.singletonList("available")));
    if(!StringUtils.isEmpty(architecture) && !ANY.equals(architecture)) {
      filters.add(new Filter("architecture", Collections.singletonList(architecture)));
    }
    if(!StringUtils.isEmpty(description)) {
      filters.add(new Filter("description", Collections.singletonList(description)));
    }
    if(!StringUtils.isEmpty(name)) {
      filters.add(new Filter("name", Collections.singletonList(name)));
    }
    if(!StringUtils.isEmpty(ownerAlias) && !ANY.equals(ownerAlias)) {
      filters.add(new Filter("owner-alias", Collections.singletonList(ownerAlias)));
    }
    if(!StringUtils.isEmpty(ownerId)) {
      filters.add(new Filter("owner-id", Collections.singletonList(ownerId)));
    }
    if(!StringUtils.isEmpty(productCode)) {
      filters.add(new Filter("product-code", Collections.singletonList(productCode)));
    }
    if(!StringUtils.isEmpty(tags)) {
      for(String tag : tags.split(";")) {
        String[] nv = tag.split("=",2);
        if(nv.length != 2) {
          LOGGER.log(Level.WARNING, "Invalid tags specification {0}", nv);
        } else {
          filters.add(new Filter("tag:"+nv[0],Collections.singletonList(nv[1])));
        }
      }
    }
    if(!StringUtils.isEmpty(shared) && !ANY.equals(shared)) {
      filters.add(new Filter("is-public", Collections.singletonList(shared)));
    }
    return filters;
  }

  /**
   * Converts {@link AwsAmiTriggerFilter} into a <code>String</code>
   * representation.
   *
   * @return string containing all fields
   */
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("architecture", architecture)
      .append("description", description)
      .append("name", name)
      .append("ownerAlias", ownerAlias)
      .append("ownerId", ownerId)
      .append("productCode", productCode)
      .append("tags", tags)
      .append("shared", shared).toString();
  }

  /**
   * A Jenkins <code>DescriptorImpl</code> for the {@link AwsAmiTriggerFilter}.
   *
   * @author Rik Turnbull
   *
   */
  @Extension
  public static final class DescriptorImpl extends Descriptor<AwsAmiTriggerFilter> {

    /**
     * Returns the trigger display name.
     * @return an empty string
     */
    @Override
    public String getDisplayName() {
      return "";
    }
  }
}
