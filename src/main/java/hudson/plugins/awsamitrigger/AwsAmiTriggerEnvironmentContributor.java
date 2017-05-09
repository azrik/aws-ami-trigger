package hudson.plugins.awsamitrigger;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;

@Extension
public final class AwsAmiTriggerEnvironmentContributor extends EnvironmentContributor {

  public void buildEnvironmentFor(Run run, EnvVars envVars, TaskListener taskListener) {
    populateEnvironment(run, envVars, taskListener);
  }

  private void populateEnvironment(Run<?,?> run, EnvVars envVars, TaskListener taskListener) {
    AwsAmiTriggerCause cause = run.getCause(AwsAmiTriggerCause.class);
    if (cause != null) {
      envVars.put("awsAmiTriggerPattern", cause.getPattern());
      envVars.put("awsAmiTriggerImageArchitecture", cause.getImage().getArchitecture());
      envVars.put("awsAmiTriggerImageCreationDate", cause.getImage().getCreationDate());
      envVars.put("awsAmiTriggerImageDescription", cause.getImage().getDescription());
      envVars.put("awsAmiTriggerImageHypervisor", cause.getImage().getHypervisor());
      envVars.put("awsAmiTriggerImageId", cause.getImage().getImageId());
      envVars.put("awsAmiTriggerImageType", cause.getImage().getImageType());
      envVars.put("awsAmiTriggerImageName", cause.getImage().getName());
      envVars.put("awsAmiTriggerOwnerId", cause.getImage().getOwnerId());
    }
  }
}
