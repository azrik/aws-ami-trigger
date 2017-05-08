package hudson.plugins.awsamitrigger;

import com.amazonaws.services.ec2.model.Image;

import hudson.model.Cause;

public final class AwsAmiTriggerCause extends Cause {

  private final String pattern;
  private final Image image;

  public AwsAmiTriggerCause(AwsAmiTrigger trigger, Image image) {
    this.pattern = trigger.getPattern();
    this.image = image;
  }

  @Override
  public String getShortDescription() {
    return Messages.Cause(pattern, image.getDescription());
  }
}
