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
   * Create a new {@link AwsAmiTriggerFilter}.
   *
   * @param architecture
   *          image architecture (i386|x86_64)
   * @param description
   *          description of image (provided during image creation)
   * @param name
   *          name of ami (may be a wildcard)
   * @param ownerAlias
   *          the AWS account alias (for example, amazon)
   * @param ownerId
   *          the AWS account id of the image owner
   * @param productCode
   *          the product code
   * @param tags
   *          the key/value combination of a tag assigned to the resource
   * @param shared
   *          aka is public
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
    LOGGER.log(Level.INFO, toString());
  }

  public String getArchitecture() {
    return architecture;
  }

  public String getDescription() {
    return description;
  }

  public String getName() {
    return name;
  }

  public String getOwnerAlias() {
    return ownerAlias;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public String getProductCode() {
    return productCode;
  }

  public String getTags() {
    return tags;
  }

  public String isShared() {
    return shared;
  }

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
          filters.add(new Filter(nv[0],Collections.singletonList(nv[1])));
        }
      }
    }
    if(!StringUtils.isEmpty(shared) && !ANY.equals(shared)) {
      filters.add(new Filter("is-public", Collections.singletonList(shared)));
    }
    return filters;

  }
  //    request.setFilters(Collections.singleton(new Filter("name",Collections.singletonList(pattern))));

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

  @Extension
  public static final class DescriptorImpl extends Descriptor<AwsAmiTriggerFilter> {
    @Override
    public String getDisplayName() {
      return "";
    }
  }
}
