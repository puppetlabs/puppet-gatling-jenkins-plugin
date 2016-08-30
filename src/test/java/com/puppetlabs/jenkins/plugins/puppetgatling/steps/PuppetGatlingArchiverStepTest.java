package com.puppetlabs.jenkins.plugins.puppetgatling.steps;

import com.puppetlabs.jenkins.plugins.puppetgatling.PuppetGatlingBuildAction;
import com.puppetlabs.jenkins.plugins.puppetgatling.PuppetGatlingProjectAction;
import com.puppetlabs.jenkins.plugins.puppetgatling.gatling.SimulationConfig;
import com.puppetlabs.jenkins.plugins.puppetgatling.gatling.SimulationReport;
import hudson.model.Action;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PuppetGatlingArchiverStepTest extends Assert {

    private static final String RESOURCES_DIR = "./src/test/resources/com/puppetlabs/jenkins/plugins/puppetgatling/steps/PuppetGatlingArchiverStepTest";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * Test archiving of gatling reports
     */
    @Test
    public void archive() throws Exception {
        // job setup
        WorkflowJob foo = j.jenkins.createProject(WorkflowJob.class, "foo");
        String sampleWorkspaceDir = new File(RESOURCES_DIR, "workspace").getAbsolutePath();
        foo.setDefinition(new CpsFlowDefinition(StringUtils.join(Arrays.asList(
                "node {",
                "  sh 'pwd'",
                "  sh 'cp -r " + sampleWorkspaceDir + "/* .'",
                "  sh 'sleep 1'",
                "  sh 'ls -l'",
                "  puppetGatlingArchive()",
                "}"), "\n")));

        // get the build going, and wait until workflow pauses
        WorkflowRun b = j.assertBuildStatusSuccess(foo.scheduleBuild2(0).get());
        List<PuppetGatlingBuildAction> buildActions = b.getActions(PuppetGatlingBuildAction.class);
        assertEquals("Should contain 1 PuppetGatlingBuildAction",
                1, buildActions.size());
        PuppetGatlingBuildAction pgba = buildActions.get(0);

        assertEquals("/plugin/puppet-gatling-jenkins-plugin/img/puppet.png", pgba.getIconFileName());
        assertEquals("Puppet Gatling", pgba.getDisplayName());
        assertEquals("puppet-gatling", pgba.getUrlName());
        assertEquals("job/foo/1/puppet-gatling/report/my-sim", pgba.getReportURL("my-sim"));

        List<SimulationReport> sims = pgba.getSimulationReportList();
        assertEquals("Should have one simulation",
                1, sims.size());
        SimulationReport sim = sims.get(0);
        assertEquals("my-sim", sim.getName());
        assertEquals("Intel(R) Xeon(R) CPU E3-1280 v3 @ 3.60GHz", sim.getSpeedOfCPU());
        assertEquals("8", sim.getNumCPUs());
        assertEquals("4.5.2", sim.getPuppetVersion());
        assertEquals("931.48 GiB", sim.getDiskSizeBytes());
        assertEquals("23.23 GiB", sim.getMemSize());
        assertEquals("beaker (2.48.1)", sim.getBeakerVersion());
        assertEquals("a7762e7069a52706ec67d827fad64741aeefd14b", sim.getGatlingPuppetLoadTestSHA());
        assertEquals("PECouchPerfMedium,3,1\n", sim.getSimConfigDataString());

        List<SimulationConfig> simConfigs = sim.getSimulationConfig();
        assertEquals(1, simConfigs.size());
        SimulationConfig simConfig = simConfigs.get(0);
        assertEquals("PECouchPerfMedium", simConfig.getSimulationName());
        assertEquals(3, simConfig.getNumberInstances());
        assertEquals(1, simConfig.getNumberRepetitions());

        assertEquals(new Long(5803), sim.getTotalMeanAgentRunTime());
        assertEquals(0, sim.getTotalFailedRequests());
        assertEquals(new Long(4447), sim.getTotalMeanCatalogResponseTime());
        assertEquals(new Long(457), sim.getTotalReportResponseTime());
        // TODO: test these
//        assertEquals("node sim data", sim.getNodeSimulationData());
//        assertEquals("node sim response times", sim.getNodeMeanResponseTimes());
        assertEquals(new Long(899), sim.getOtherResponseTime());

        assertEquals("[{label:\"memory\"}]", sim.getMetrics().getMemoryUsage().getSeriesNamesJSON());
        assertEquals("[[[1,2934],[2,3234],[3,2160],[4,2865],[5,2994],[6,2793],[7,2935],[8,2925],[9,2914],[10,2961],[11,2968],[12,3007],[13,2998],[14,2962],[15,2922],[16,2938],[17,2994]]]",
                sim.getMetrics().getMemoryUsage().getSeriesJSON());

        Collection<? extends Action> projectActions = pgba.getProjectActions();
        assertEquals("Should contain 1 project Action",
                1, projectActions.size());
        Action firstProjectAction = projectActions.iterator().next();
        assertEquals("Project action should be a PuppetGatlingProjectAction",
                PuppetGatlingProjectAction.class, firstProjectAction.getClass());
        PuppetGatlingProjectAction projectAction = (PuppetGatlingProjectAction) firstProjectAction;

        assertEquals("/plugin/puppet-gatling-jenkins-plugin/img/puppet.png", projectAction.getIconFileName());
        assertEquals("Puppet Gatling", projectAction.getDisplayName());
        assertEquals("puppet-gatling", projectAction.getUrlName());
        assertEquals("1/gatling/report/my-sim", projectAction.getReportURL(1, "my-sim"));
        // TODO: test these (graphs)
//        assertEquals("agent runtime graph", projectAction.getAgentRunTime());
//        assertEquals("catalog compile time graph", projectAction.getCatalogCompileTime());
//        assertEquals("report request time graph", projectAction.getReportRequestTime());
//        assertEquals("failed requests graph", projectAction.getFailedRequests());

    }
}
