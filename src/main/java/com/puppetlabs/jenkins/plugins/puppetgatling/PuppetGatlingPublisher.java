package com.puppetlabs.jenkins.plugins.puppetgatling;

import static com.puppetlabs.jenkins.plugins.puppetgatling.Constant.*;
import com.puppetlabs.jenkins.plugins.puppetgatling.gatling.GatlingReportArchiver;
import com.puppetlabs.jenkins.plugins.puppetgatling.gatling.PuppetGatlingBuildAction;
import com.puppetlabs.jenkins.plugins.puppetgatling.gatling.SimulationReport;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.*;

import hudson.model.Run;
import hudson.model.TaskListener;
import io.gatling.jenkins.BuildSimulation;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import javax.annotation.Nonnull;

/**
 * <h2>Puppet Gatling Publisher</h2>
 *
 * This is where the majority of the logic resides for this plugin.
 *
 * <br><br>
 * Related .jelly file<br>
 * 	<ul>
 * 	    <li>config.jelly</li>
 * 	</ul>
 *
 * <h3>config.jelly</h3>
 * 	This file is responsible for the GUI element found when adding the plugin
 * 	as a post-build step.
 *
 * @author Brian Cain
 */
public class PuppetGatlingPublisher extends Recorder implements SimpleBuildStep {

    private static final HashSet<String> PIE_CHART_CATEGORIES = new HashSet<String>(Arrays.asList("catalog", "report"));

    private boolean deployEvenBuildFail;
    private PrintStream logger;

    // New constructor
    @DataBoundConstructor
    public PuppetGatlingPublisher(boolean deployEvenBuildFail) {
        this.deployEvenBuildFail = deployEvenBuildFail;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> project) {
        return Arrays.asList(new PuppetGatlingProjectAction(project));
    }

    private boolean isPerformDeployment(AbstractBuild build) {
        Result result = build.getResult();
        if (result == null) {
            return true;
        }

        if (deployEvenBuildFail) {
            return true;
        }

        return build.getResult().isBetterOrEqualTo(Result.UNSTABLE);
    }

    /**
     * This is the entry point for where the plugin starts once a job is executed after being added as a
     * "post-build step" on jenkins.
     * @param run Object that contains data relating to reports, jobs, etc
     * @param workspace the path to the jenkins workspace
     * @param launcher the jenkins launcher
     * @param listener Where the logger is located
     * @throws InterruptedException if the operation is interrupted
     * @throws IOException if an IO error occurs during the build
     */
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        logger = listener.getLogger();
        logger.println("[PuppetGatling] - Starting deployment from the post-action ...");

        boolean success = getBuildAction(run, workspace);

        if (!success){
            logger.println("[PuppetGatling] - Get Build Action failed.");
            run.setResult(Result.FAILURE);
        }
    }

    /**
     * getbuildAction grabs all of the GatingBuildAction objects within build which
     * is then extracted into a local GatlingBuildAction object. Then we
     * iterate over all the available reports from the GatlingBuildAction object
     * to obtain and parse the stats.tsv file within each report. Once it's parsed, and the calculations are made,
     * we add the values to the simulationreport, with it's given name, and add it to our report list. That report
     * list is then added to our build action.
     *
     * @param run the jenkins Run
     * @param workspace the path to the jenkins workspace
     * @return boolean of if it worked or not
     * @throws InterruptedException if the operation is interrupted
     * @throws IOException if an IO error occurs during the build
     */
    private boolean getBuildAction(Run<?, ?> run, FilePath workspace) throws IOException, InterruptedException{
        GatlingReportArchiver archiver = new GatlingReportArchiver();
        List<BuildSimulation> simulations = archiver.saveFullReports(run, this.logger, workspace);
        if (simulations.size() == 0){
            return false;
        }


        List<SimulationReport> simulationReportList = new ArrayList<SimulationReport>();
        Map<String, NodeSimulationData> simulationData;

        for (BuildSimulation sim : simulations){

            List<SimulationConfig> simConfig = getGatlingSimData(workspace, sim.getSimulationName());
            for (SimulationConfig sc : simConfig){
                this.logger.println("[PuppetGatling] - Here are the Gatling Simulation Results for " + sim.getSimulationName() + ": " + sc.getSimulationName() + ", "
                        + sc.getNumberInstances() + ", " + sc.getNumberRepetitions());
            }

            FilePath statsJs = new FilePath(sim.getSimulationDirectory(), "js/stats.js");

            this.logger.println("[PuppetGatling] - The stats javascript file is: " + statsJs);

            // new hash with data ready to be calculated
            // This could be where I pass in the appropriate SimulationConfig data structure
            // so it can be added the Map simulationData
            simulationData = getGroupCalculations(statsJs);

            SimulationReport simulationReport = generateSimulationReport(new SimulationReport(),
                    simulationData, workspace, sim.getSimulationName(), simConfig);
            simulationReportList.add(simulationReport);
        }

        PuppetGatlingBuildAction customAction =
                new PuppetGatlingBuildAction(run, simulations, simulationReportList);
        run.addAction(customAction);
        return true;
    }

    /**
     * getGroupCalculations parses the stats.tsv file to separate calculation by groups, then calculates
     * the given values required by our plugin.
     *
     * @param statsJsFilePath - path to stats.gs that is generated by Gatling reports
     * @return - A HashMap of key String and value Array of Strings, where the key is a given group and the value is an array of stats per line from stats.tsv
     * @throws IOException if an IO error occurs while calculating the data
     */
    private Map<String, NodeSimulationData> getGroupCalculations(FilePath statsJsFilePath) throws IOException, InterruptedException {

        // TODO: the data structures in here could really use some cleanup / simplification,
        //  but for now I'm trying to touch as little code as possible to get things
        //  up and running with Gatling 2.0.
        Map<String, NodeSimulationData> groupDict = new HashMap<String, NodeSimulationData>();

        // NOTE: This would be the preferred way to do the Javascript evaluation,
        // but something about the Jenkins classloader is causing it to return
        // `null` for the ScriptEngine, even though this works fine in tests.  For
        // now I've decided to workaround by calling directly into the Rhino API,
        // but it would be nice to clean this up at some point.
//        ScriptEngineManager manager = new ScriptEngineManager();
//        ScriptEngine engine = manager.getEngineByName("javascript");
//        try {
//            engine.eval(new InputStreamReader(statsJsFilePath.read()));
//        } catch (ScriptException e) {
//            throw new IllegalStateException("Error evaluating stats.js file", e);
//        }
//        Map stats = (Map) engine.get("stats");

        Context cx = Context.enter();
        Map stats;
        try {
            cx.setLanguageVersion(Context.VERSION_1_8);
            Scriptable scope = cx.initStandardObjects();
            Object result = cx.evaluateReader(scope, new InputStreamReader(statsJsFilePath.read(), "UTF-8"),
                    "stats.js", 1, null);
            stats = (Map) scope.get("stats", scope);

        } finally {
            Context.exit();
        }

        Map contents = (Map) stats.get("contents");
        for (Object contentEntry : contents.values()) {
            Map contentEntryMap = (Map)contentEntry;
            String type = (String) contentEntryMap.get("type");

            if (type.equals("GROUP")) {
                String group = (String) contentEntryMap.get("name");
                SimulationData groupData = parseSimulationData(group, "", (Map) contentEntryMap.get("stats"));

                ArrayList<SimulationData> requestData = new ArrayList<SimulationData>();

                requestData.addAll(parseRequestStats(group, (Map) contentEntryMap.get("contents")));

                groupDict.put(group, new NodeSimulationData(groupData, requestData));
            } else {
                throw new IllegalStateException("Unrecognized content type: '" + type + "'");
            }
        }

        logger.println("[PuppetGatling] - The hash map is below.");
        logger.println("[PuppetGatling] - The values are printed as: [Total Requests, Successful Requests, Failed Requests, Mean Response Time]");
        for (Map.Entry groupEntry : groupDict.entrySet()){;
            NodeSimulationData nsd = groupDict.get(groupEntry.getKey());
            logger.println("[PuppetGatling] - Node group summary data for group '" +
                    groupEntry.getKey() + "': " + nsd.getGroupData().prettyPrint());
            for (SimulationData sd : nsd.getRequestData()){
                logger.println("[PuppetGatling] - The hash map key, value is: " + groupEntry.getKey() + ", " + sd.prettyPrint());
            }

        }

        return groupDict;
    }

    private List<SimulationData> parseRequestStats(String origGroup, Map contents) {
        List<SimulationData> results = new ArrayList<SimulationData>();

        for (Object contentEntry : contents.values()) {
            Map contentEntryMap = (Map) contentEntry;
            String type = (String) contentEntryMap.get("type");

            if (type.equals("REQUEST")) {
                String path = (String) contentEntryMap.get("path");
                String[] pathSplit = path.split(" / ");
                String group = pathSplit[0];
                String request = pathSplit[1];
                logger.println("FOUND REQUEST: '" + group + "', '" + request + "'");
                if (!group.equals(origGroup)) {
                    throw new IllegalStateException("Expected group '" + origGroup + "', found '" + group + "'");
                }

                results.add(parseSimulationData(group, request, (Map) contentEntryMap.get("stats")));

            } else {
                throw new IllegalStateException("Unrecognized content type: '" + type + "'");
            }
        }
        return results;
    }

    private SimulationData parseSimulationData(String group, String request, Map stats) {
        Map numRequests = (Map) stats.get("numberOfRequests");
        int totalRequests = Integer.parseInt((String) numRequests.get("total"));
        int succReqs = Integer.parseInt((String) numRequests.get("ok"));
        int failedReqs = Integer.parseInt((String) numRequests.get("ko"));
        Map meanResponseTime = (Map) stats.get("meanResponseTime");
        int meanResp = Integer.parseInt((String) meanResponseTime.get("total"));

        return new SimulationData(group, request, totalRequests,
                        succReqs, failedReqs, meanResp);
    }


    /**
     * Generates the SimulationReport that will be added as an artifact
     *
     * @param simReport - Given Simulation Report structure
     * @param simulationData - List of information related to the simulation
     * @param workspace - workspace path
     * @param simID - Unique simulation ID
     * @param simConfig  - List of given configs for the whole simulation
     * @return a new simulation report
     * @throws IOException
     */
    private SimulationReport generateSimulationReport(SimulationReport simReport, Map<String, NodeSimulationData> simulationData, FilePath workspace, String simID, List<SimulationConfig> simConfig) throws IOException, InterruptedException {
        logger.println("[PuppetGatling] - Generating simulation report data...");
        FilePath osData = new FilePath(workspace, "puppet-gatling/" + simID + "/important_data.csv");
        LineIterator it = IOUtils.lineIterator(osData.read(), "UTF-8");

        simReport.setName(simID);
        simReport.setNodeSimulationData(simulationData);

        try{
            while(it.hasNext()){
                String line = it.nextLine();
                logger.println("PARSING LINE: '" + line + "'");
                String[] tokens = line.split(",");
                String key = tokens[0];
                String osStatistic = tokens[1];
                if (key.equals("memorysize")){
                    simReport.setMemSize(osStatistic);
                }
                else if (key.equals("processor0")){
                    simReport.setSpeedOfCPU(osStatistic);
                }
                else if (key.equals("processorcount")){
                    simReport.setNumCPUs(osStatistic);
                }
                else if (key.equals("puppetversion")){
                    simReport.setPuppetVersion(osStatistic);
                }
                else if (key.equals("beaker-version")){
                    simReport.setBeakerVersion(osStatistic);
                }
                else if (key.equals("gatling-puppet-load-test")){
                    simReport.setGatlingPuppetLoadTestSHA(osStatistic);
                }
                else if (key.equals("blockdevice_sda_size")){
                    simReport.setDiskSizeBytes(osStatistic);
                }
            }
        } finally{
            it.close();
            logger.println("[PuppetGatling] - OS Data saved.");
        }

        FilePath facterDataPath = new FilePath(workspace, "puppet-gatling/" + simID + "/gatling_sim_data.csv");
        String facterData = IOUtils.toString(facterDataPath.read(), "UTF-8");
        simReport.setFacterData(facterData);
        logger.println("[PuppetGatling] - Facter data saved.");

        // Get data from file in puppet-gatlin/ text file, generated by ruby
        // place the rest of the data in there

        simReport.setSimulationConfig(simConfig);

        // do calculations
        simReport = calculateDataPerNode(simReport);

        simReport = calculateDataPerSimulation(simReport);

        return simReport;
    }

    /**
     * For each node per simulation, calculate the mean response time, add it to a dictionary where the
     * key is the node name and the value is the mean response time, then add that to the simulation report.
     *
     * This function also grabs a Key, Value pair for Catalog and Report response times.
     *
     * @param simulationReport  - A simulation report with relevant data stats from simulation Data
     * @return a new simulation report with the calculated data
     */
    private SimulationReport calculateDataPerNode(SimulationReport simulationReport){
        // should return list, since it's per node
        int meanRunTimePerNode;
        int totalFailedRequests = 0;
        Map<String, Map<String, Long>> nodeMeanResponseTimes = new HashMap<String, Map<String, Long>>();

        // This seems like it may be overly complex; it seems like we're keeping a handle to several
        // different data structures when a single map might suffice.  We also probably should clean
        // up the class / method names a bit to make it more clear what the cardinality is between
        // a "Simulation" and a "Node".
        List<SimulationConfig> simulationConfig = simulationReport.getSimulationConfig();
        Map<String, NodeSimulationData> simulationData = simulationReport.getNodeSimulationData();

        for (Map.Entry<String, NodeSimulationData> entry : simulationData.entrySet()) {
            String groupName = entry.getKey();
            NodeSimulationData nodeSimulationData = entry.getValue();
            SimulationData groupData = nodeSimulationData.getGroupData();
            List<SimulationData> requestData = nodeSimulationData.getRequestData();
            if (requestData.size() > 0){
                for (SimulationData sd : requestData){
                    logger.println("[PuppetGatling] - Getting mean run time for: " + sd.getKey() + " (" + sd.getMeanResponseTime() + ")");
                    totalFailedRequests += sd.getFailedRequests();

                    String cat = sd.getRequestName().trim();
                    if (PIE_CHART_CATEGORIES.contains(cat)) {
                        addNodeMeanResponseTime(nodeMeanResponseTimes, sd.getKey(), cat, (long) sd.getMeanResponseTime());
                    }
                }

                logger.println("Here are the mean response times:");
                for (Map.Entry<String, Map<String, Long>> node : nodeMeanResponseTimes.entrySet()) {
                    logger.println("\tNODE: '" + node.getKey() + "'");
                    for (String cat : node.getValue().keySet()) {
                        logger.println("\t\t" + cat + ": '" + node.getValue().get(cat) + "'");
                    }
                }


                SimulationConfig localSimConfig = getSimConfig(simulationConfig, entry.getKey());
                if (localSimConfig == null){
                    // needs a better way to quit out of this
                    logger.println("[PuppetGatling] - ERROR: There is no sim config by the name '" + entry.getKey() + "'");
                    throw new IllegalStateException("[PuppetGatling] - ERROR: There is no sim config by that name");
                }
                else{
                    meanRunTimePerNode = groupData.getMeanResponseTime();
                    logger.println("[PuppetGatling] - Here is the mean run time per node of " + localSimConfig.getSimulationName() + ": " + meanRunTimePerNode);


                    addNodeMeanResponseTime(nodeMeanResponseTimes, getSimConfig(simulationConfig, entry.getKey()).getSimulationName(), "agent", meanRunTimePerNode);

                    // TODO: I don't know if this is exactly right.  I think that
                    // when we're in this loop, we, are looping over the
                    // Sims, and there may be multiple nodes in a sim?
                    String node = getSimConfig(simulationConfig, entry.getKey()).getSimulationName();
                    Map<String, Long> times = nodeMeanResponseTimes.get(node);
                    int sum = 0;
                    for (String cat : PIE_CHART_CATEGORIES) {
                        if (times.containsKey(cat)) {
                            logger.println("About to look up time for node '" + node + "', cat: '" + cat + "'");
                            sum += times.get(cat);
                        } else {
                            logger.println("[PuppetGatling] - ERROR: Request \"" + cat + "\" does not have an associated time.  The request likely failed.");
                        }
                    }
                    long otherTime = meanRunTimePerNode - sum;
                    addNodeMeanResponseTime(nodeMeanResponseTimes, node, "other", otherTime);
                }
            }
        }

        simulationReport.setNodeMeanResponseTimes(nodeMeanResponseTimes);

        simulationReport.setTotalFailedRequests(totalFailedRequests);
        return simulationReport;
    }

    /**
     * Adds data to the TotalNodeMap. Has to append by grabing old list, appending new value to the list, and reseting
     * the key to the new appended list.
     *
     * @param nodeMeanResponseTimes - A Map that contains all the information for all nodes in the simulation
     * @param nodeName - The name of the node
     * @param key - The name of the category of response time that we're updating
     * @param value - The mean response time for this node in this category
     */
    private void addNodeMeanResponseTime(Map<String, Map<String, Long>> nodeMeanResponseTimes,
                                         String nodeName, String key, long value) {
        // TODO: this whole method can probably go away
        if (!nodeMeanResponseTimes.containsKey(nodeName)) {
            nodeMeanResponseTimes.put(nodeName, new HashMap<String, Long>());
        }

        nodeMeanResponseTimes.get(nodeName).put(key, value);
    }

    /**
     * Search through the config list for the config that matches the given key, so correct numbers are used
     * on node calculations
     *
     * @param simulationConfigList - A list of all the simulation configs
     * @param key - Key we are looking for
     * @return returns the discovered sim config, else null if not found
     */
    private SimulationConfig getSimConfig(List<SimulationConfig> simulationConfigList, String key){
        logger.println("Looking for simconfig: '" + key + "'");
        for (SimulationConfig simConf : simulationConfigList){
            logger.println("\tfound simulation '" + simConf.getSimulationName() + "'");
            if (simConf.getSimulationName().equals(key)){
                return simConf;
            }
        }
        logger.println("Did not find simulation config!");
        return null;
    }

    /**
     *
     * Calculates the agent, catalog, and report total mean response time for a given simulation report.
     *
     * @param simulationReport - A given simulation report to calculate the data with
     * @return a new simulation report
     */
    private SimulationReport calculateDataPerSimulation(SimulationReport simulationReport){
        Long numerator = 0L, denominator = 0L, catalogNumerator = 0L, reportNumerator = 0L;

        Map<String, Map<String, Long>> maps = simulationReport.getNodeMeanResponseTimes();
        Set<Map.Entry<String, Map<String, Long>>> responseTimeEntrySet = maps.entrySet();
        for(Map.Entry<String, Map<String, Long>> nodeEntry : responseTimeEntrySet){
            Map<String, Long> means = nodeEntry.getValue();
            SimulationConfig simConf = getSimConfig(simulationReport.getSimulationConfig(), nodeEntry.getKey());
            Set<Map.Entry<String, Long>> meanEntrySet = means.entrySet();
            for(Map.Entry<String, Long> meanEntry : meanEntrySet){
                String meanKey = meanEntry.getKey();
                Long meanValue = meanEntry.getValue();
                if (meanKey.equals("agent")){
                    numerator += (simConf.getNumberInstances() * simConf.getNumberRepetitions()) * meanValue;
                }
                else if (meanKey.equals("catalog")){
                    catalogNumerator += (simConf.getNumberInstances() * simConf.getNumberRepetitions()) * meanValue;
                }
                else if (meanKey.equals("report")){
                    reportNumerator += (simConf.getNumberInstances() * simConf.getNumberRepetitions()) * meanValue;
                }
            }
            denominator += (long) simConf.getNumberInstances() * simConf.getNumberRepetitions();
        }

        if (denominator > 0){
            simulationReport.setTotalMeanAgentRunTime((numerator / denominator));
            simulationReport.setTotalMeanCatalogResponseTime((catalogNumerator / denominator));
            simulationReport.setTotalReportResponseTime((reportNumerator / denominator));
            logger.println("[PuppetGatling] - The Agent total mean response time for " + simulationReport.getName() + ": " + simulationReport.getTotalMeanAgentRunTime());
            logger.println("[PuppetGatling] - The Catalog total mean response time for " + simulationReport.getName() + ": " + simulationReport.getTotalMeanCatalogResponseTime());
            logger.println("[PuppetGatling] - The Report total mean response time for " + simulationReport.getName() + ": " + simulationReport.getTotalReportResponseTime());
        }

        return simulationReport;
    }

    private Long getResponseTime(Map<String, Long> responseList, String key){
        for (Map.Entry entry : responseList.entrySet()){
            if (entry.getKey().equals(key)){
                return Long.parseLong(entry.getValue().toString());
            }
        }
        return null;
    }

    /**
     * Finds data stored by gatling-puppet-load-test and saves it as the simulation config.
     *
     * @param workspace - workspace directory
     * @param simID - simulation id, used to determine where the gatling sim data is saved on disk
     * @return - a new simulation configuration
     * @throws IOException
     */
    private List<SimulationConfig> getGatlingSimData(FilePath workspace, String simID) throws IOException, InterruptedException {
        // needs simulation name for folder name
        FilePath simJsonData = new FilePath(workspace, "puppet-gatling/" + simID + "/gatling_sim_data.csv");
        List<SimulationConfig> simConfig = new ArrayList<SimulationConfig>();
        LineIterator it = IOUtils.lineIterator(simJsonData.read(), "UTF-8");

        logger.println("[PuppetGatling] - Getting simulation configuration data...");

        try{
            while(it.hasNext()){
                String line = it.nextLine();
                if (line.length() > 0){
                    String[] tokens = line.split(",");

                    String simulationName = tokens[0];
                    int numberInstances = Integer.parseInt(tokens[1]);
                    int numberRepetitions = Integer.parseInt(tokens[2]);

                    simConfig.add(new SimulationConfig(simulationName, numberInstances, numberRepetitions));
                }
            }

        } finally{
            it.close();
            logger.println("[PuppetGatling] - Got it!");
        }

        return simConfig;
    }

     public boolean isDeployEvenBuildFail() {
         return deployEvenBuildFail;
     }

     public void setDeployEvenBuildFail(boolean deployEvenBuildFail) {
         this.deployEvenBuildFail = deployEvenBuildFail;
     }

    @Extension
    public static final class PuppetGatlingDescriptor extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
