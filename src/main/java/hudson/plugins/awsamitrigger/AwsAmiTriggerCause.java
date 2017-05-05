package hudson.plugins.awsamitrigger;

import hudson.model.Cause;

public final class AwsAmiTriggerCause extends Cause {

  private final String credentialsId;
  private final String regionName;
  private final String pattern;

  public AwsAmiTriggerCause(AwsAmiTrigger config) {
    this.credentialsId = config.getCredentialsId();
    this.regionName = config.getRegionName();
    this.pattern = config.getPattern();
  }

  public String getCredentialsId() {
    return credentialsId;
  }

  public String getRegionName() {
    return regionName;
  }

  public String getPattern() {
    return pattern;
  }

  @Override
  public String getShortDescription() {
    return Messages.Cause(pattern);
  }
}
