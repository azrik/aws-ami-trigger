/*
  MIT License

  Copyright (c) 2017 Rik Turnbull

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
*/
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
