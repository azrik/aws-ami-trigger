package hudson.plugins.awsamitrigger;

import com.amazonaws.services.ec2.model.Image;

import hudson.model.Cause;

public final class AwsAmiTriggerCause extends Cause {

  private final Image image;

  public AwsAmiTriggerCause(AwsAmiTrigger trigger, Image image) {
    this.image = image;
  }

  public Image getImage() {
    return image;
  }

  @Override
  public String getShortDescription() {
    return Messages.Cause(image.getDescription());
  }
}
