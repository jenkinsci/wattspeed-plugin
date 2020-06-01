package io.jenkins.plugins.wattspeed;

import hudson.model.FreeStyleProject;
import hudson.util.Secret;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class WattspeedBuilderTest
{
  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  final String token = "ZBOknBYGBDOS8hz7kSjH/R1lPEnUx3ztph2kyFN4A9AfM3i1aMPKKJPRgPawS+aB";
  final int project_id = 1;

  @Test
  public void testBuild() throws Exception
  {
    FreeStyleProject project = jenkins.createFreeStyleProject();
    WattspeedBuilder builder = new WattspeedBuilder(Secret.fromString(token), project_id);
    project.getBuildersList().add(builder);

    jenkins.buildAndAssertSuccess(project);
  }
}