package hudson.plugins.awsamitrigger;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import jenkins.model.Jenkins;

public final class AwsAmiTriggerFilter extends AbstractDescribableImpl<AwsAmiTriggerFilter> {

  private static final Logger LOGGER = Logger.getLogger(AwsAmiTriggerFilter.class.getName());

  private final String name;

  /**
   * Create a new {@link AwsAmiTriggerFilter}.
   *
   * @param name
   *          name of ami (may be a wildcard)
   */
  @DataBoundConstructor
  public AwsAmiTriggerFilter(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Extension
  public static final class DescriptorImpl extends Descriptor<AwsAmiTriggerFilter> {
    @Override
    public String getDisplayName() {
      return "";
    }
  }
}
