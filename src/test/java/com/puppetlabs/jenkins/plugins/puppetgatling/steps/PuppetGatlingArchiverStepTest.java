package com.puppetlabs.jenkins.plugins.puppetgatling.steps;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Arrays;

public class PuppetGatlingArchiverStepTest extends Assert {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * Test archiving of gatling reports
     */
    @Test
    public void archive() throws Exception {
        // job setup
        WorkflowJob foo = j.jenkins.createProject(WorkflowJob.class, "foo");
        foo.setDefinition(new CpsFlowDefinition(StringUtils.join(Arrays.asList(
                "node {",
                "  puppetGatlingArchive()",
                "}"), "\n")));

        // get the build going, and wait until workflow pauses
        WorkflowRun b = j.assertBuildStatusSuccess(foo.scheduleBuild2(0).get());

        assertEquals(true, true);
    }
}