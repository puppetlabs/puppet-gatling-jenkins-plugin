package com.puppetlabs.jenkins.plugins.puppetgatling;

import static com.puppetlabs.jenkins.plugins.puppetgatling.Constant.*;
import com.excilys.ebi.gatling.jenkins.BuildSimulation;
import com.excilys.ebi.gatling.jenkins.GatlingBuildAction;
import com.puppetlabs.jenkins.plugins.puppetgatling.gatling.PuppetGatlingBuildAction;
import com.puppetlabs.jenkins.plugins.puppetgatling.gatling.SimulationReport;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

/**
 * <h2>Puppet Gatling Publisher</h2>
 *
 * This is where the majority of the logic resides for this plugin.
 *
 * <br></br><br></br>
 * Related .jelly file<br></br>
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
public class PuppetGatlingPublisher extends Recorder implements Serializable{

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
     * @param build Object that contains data relating to reports, jobs, etc
     * @param launcher
     * @param listener Where the logger is located
     * @return Returns true or false depending on success of getBuildAction
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        logger = listener.getLogger();
        logger.println("[PuppetGatling] - Starting deployment from the post-action ...");

        boolean success = getBuildAction(build, launcher, listener);

        if (!success){
            logger.println("[PuppetGatling] - Get Build Action failed.");
            return success;
        }

        return success;
    }

    /**
     * getbuildAction grabs all of the GatingBuildAction objects within build which
     * is then extracted into a local GatlingBuildAction object. Then we
     * iterate over all the available reports from the GatlingBuildAction object
     * to obtain and parse the stats.tsv file within each report. Once it's parsed, and the calculations are made,
     * we add the values to the simulationreport, with it's given name, and add it to our report list. That report
     * list is then added to our build action.
     *
     * @param build
     * @param launcher
     * @param listener The logger
     * @return boolean of if it worked or not
     * @throws IOException
     * @throws InterruptedException
     */
    private boolean getBuildAction(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException{
        List<GatlingBuildAction> gatlingBuildActionList = build.getActions(GatlingBuildAction.class);

        if (gatlingBuildActionList.size() == 0){
            return false;
        }
        GatlingBuildAction action = gatlingBuildActionList.get(0);

        List<SimulationReport> simulationReportList = new ArrayList<SimulationReport>();
        Map<String, List<SimulationData>> simulationData = new HashMap<String, List<SimulationData>>();

        for (BuildSimulation sim : action.getSimulations()){

            List<SimulationConfig> simConfig = getGatlingSimData(build.getWorkspace(), sim.getSimulationName());
            for (SimulationConfig sc : simConfig){
                logger.println("[PuppetGatling] - Here are the Gatling Simulation Results for " + sim.getSimulationName() + ": " + sc.getSimulationName() + ", "
                        + sc.getNumberInstances() + ", " + sc.getNumberRepetitions());
            }

            FilePath simdir = new FilePath(sim.getSimulationDirectory(), "stats.tsv");

            logger.println("[PuppetGatling] - The simulation directory is: " + simdir);

            // new hash with data ready to be calculated
            // This could be where I pass in the appropriate SimulationConfig data structure
            // so it can be added the Map simulationData
            simulationData = getGroupCalculations(simdir);

            SimulationReport simulationReport = generateSimulationReport(new SimulationReport(), simulationData, build.getWorkspace(), sim.getSimulationName(), simConfig);
            simulationReportList.add(simulationReport);
        }

        PuppetGatlingBuildAction customAction = new PuppetGatlingBuildAction(build, simulationReportList);
        build.addAction(customAction);
        return true;
    }

    /**
     * getGroupCalculations parses the stats.tsv file to separate calculation by groups, then calculates
     * the given values required by our plugin.
     *
     * @param statsFilePath - path to stats.tsv that is generated by Gatling reports
     * @return - A HashMap of key String and value Array of Strings, where the key is a given group and the value is an array of stats per line from stats.tsv
     * @throws IOException
     */
    private Map<String, List<SimulationData>> getGroupCalculations(FilePath statsFilePath) throws IOException {
        Map<String, List<SimulationData>> groupDict = new HashMap<String, List<SimulationData>>();

        LineIterator it = IOUtils.lineIterator(statsFilePath.read(), "UTF-8");

        try{
            while(it.hasNext()){
                String line = it.nextLine();
                String[] tmp_toke = line.split("\t");
                if (!tmp_toke[0].equals("name") && tmp_toke.length > 1){
                    if (!tmp_toke[GATLING_STATS_INDEX_GROUP_STAT].contains("/")){
                        String key = tmp_toke[GATLING_STATS_INDEX_GROUP_STAT];
                        groupDict = appendDataDictionary(groupDict, key, tmp_toke, "");
                    }
                    else {
                        String[] key_split = tmp_toke[GATLING_STATS_INDEX_GROUP_STAT].split(" / ");
                        //logger.println("[PuppetGatling] - The split key is: " + key_split[0] + ", " + key_split[1]);
                        String key = key_split[0];
                        groupDict = appendDataDictionary(groupDict, key, tmp_toke, key_split[1]);
                    }
                }
            }
        } finally{
            it.close();
        }

        logger.println("[PuppetGatling] - The hash map is below.");
        logger.println("[PuppetGatling] - The values are printed as: [Total Requests, Successful Requests, Failed Requests, Mean Response Time]");
        for (Map.Entry entry : groupDict.entrySet()){;
            List<SimulationData> lst = groupDict.get(entry.getKey());
            for (SimulationData sd : lst){
                logger.println("[PuppetGatling] - The hash map key, value is: " + entry.getKey() + ", " + sd.prettyPrint());
            }

        }

        return groupDict;
    }

    /**
     * Appends a new value onto the associated key, value array list.
     *
     * @param dict - groupDict from getGroupCalculations
     * @param key - the given group from stats.tsv
     * @param tokens - a line of text from stats.tsv
     * @param stat - given stat that gatling recorded
     * @return appended dictionary
     */
    private Map<String, List<SimulationData>> appendDataDictionary(Map<String, List<SimulationData>> dict, String key, String[] tokens, String stat){
        if (!dict.containsKey(key)){
            dict.put(key, new ArrayList<SimulationData>());
        }
        else {
            List<SimulationData> values = dict.get(key);
            int totalRequests = Integer.parseInt(tokens[GATLING_STATS_INDEX_TOTAL_REQUESTS]);
            int succReq = Integer.parseInt(tokens[GATLING_STATS_INDEX_SUCCESSFUL_REQUESTS]);
            int failedReqs = Integer.parseInt(tokens[GATLING_STATS_INDEX_FAILED_REQUESTS]);
            int meanResp = Integer.parseInt(tokens[GATLING_STATS_INDEX_MEAN_RESPONSE_TIME]);

            SimulationData simData = new SimulationData(key, stat, totalRequests, succReq, failedReqs, meanResp);

            values.add(simData);
            dict.put(key, values);
        }

        return dict;
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
    private SimulationReport generateSimulationReport(SimulationReport simReport, Map<String, List<SimulationData>> simulationData, FilePath workspace, String simID, List<SimulationConfig> simConfig) throws IOException {
        logger.println("[PuppetGatling] - Generating simulation report data...");
        FilePath osData = new FilePath(workspace, "puppet-gatling/" + simID + "/important_data.csv");
        LineIterator it = IOUtils.lineIterator(osData.read(), "UTF-8");

        simReport.setName(simID);
        simReport.setSimulationDataList(simulationData);

        try{
            while(it.hasNext()){
                String line = it.nextLine();
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
                else if (key.equals("puppet-acceptance")){
                    simReport.setPuppetAcceptanceSHA(osStatistic);
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
        Long meanRunTimePerNode;
        int totalFailedRequests = 0;
        Map<String, List<Map<String, Long>>> totalNodeInfo = new HashMap<String, List<Map<String, Long>>>();

        List<SimulationConfig> simulationConfig = simulationReport.getSimulationConfig();
        Map<String, List<SimulationData>> simulationData = simulationReport.getSimulationDataList();

        int counter = 0;
        for (Map.Entry entry : simulationData.entrySet()){;
            List<SimulationData> lst = simulationData.get(entry.getKey());
            int numerator = 0;
            if (lst.size() > 0){
                logger.println("[PuppetGatling] - Getting mean run time for: " + lst.get(counter).getKey());
                for (SimulationData sd : lst){
                    numerator += sd.getTotalRequests() * sd.getMeanResponseTime();
                    totalFailedRequests += sd.getFailedRequests();

                    String cat = sd.getStat().trim();
                    if (cat.equals("catalog")){

                        Map<String, Long> catMap = new HashMap<String, Long>();
                        catMap.put("catalog", (long) sd.getMeanResponseTime());
                        totalNodeInfo = appendTotalNodeMap(totalNodeInfo, catMap, sd.getKey());
                    }
                    else if (cat.equals("report")){

                        Map<String, Long> reportMap = new HashMap<String, Long>();
                        reportMap.put("report", (long) sd.getMeanResponseTime());
                        totalNodeInfo = appendTotalNodeMap(totalNodeInfo, reportMap, sd.getKey());
                    }
                }
                SimulationConfig localSimConfig = getSimConfig(simulationConfig, lst.get(counter).getKey());
                if (localSimConfig == null){
                    // needs a better way to quit out of this
                    logger.println("[PuppetGatling] - ERROR: There is no sim config by that name");
                }
                else{
                    int denominator = localSimConfig.getNumberInstances() * localSimConfig.getNumberRepetitions();
                    meanRunTimePerNode = (long) (numerator / denominator);
                    logger.println("[PuppetGatling] - Here is the mean run time per node of " + localSimConfig.getSimulationName() + ": " + meanRunTimePerNode);

                    Map<String, Long> agentMap = new HashMap<String, Long>();
                    agentMap.put("agent", meanRunTimePerNode);
                    totalNodeInfo = appendTotalNodeMap(totalNodeInfo, agentMap, simulationConfig.get(counter).getSimulationName());
                    counter++;
                }
            }
        }

        simulationReport.setTotalNodeInfo(totalNodeInfo);

        simulationReport.setTotalFailedRequests(totalFailedRequests);
        return simulationReport;
    }

    /**
     * Adds data to the TotalNodeMap. Has to append by grabing old list, appending new value to the list, and reseting
     * the key to the new appended list.
     *
     * @param totalNodeInfo - A Map that contains all the information for all nodes in the simulation
     * @param dataMap - Either an agent map, catalog map, or report map to append to totalNodeInfo
     * @param simKey - Simulation key id
     * @return total node information map with new appended data
     */
    private Map<String, List<Map<String, Long>>> appendTotalNodeMap(Map<String, List<Map<String, Long>>> totalNodeInfo, Map<String, Long> dataMap, String simKey){
        if (!totalNodeInfo.containsKey(simKey)){
            List<Map<String, Long>> newMapList = new ArrayList<Map<String, Long>>();
            newMapList.add(dataMap);
            totalNodeInfo.put(simKey, newMapList);
        }
        else{
            List<Map<String, Long>> nodeData = totalNodeInfo.get(simKey);
            nodeData.add(dataMap);
            totalNodeInfo.put(simKey, nodeData);
        }

        return totalNodeInfo;
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
        for (SimulationConfig simConf : simulationConfigList){
            if (simConf.getSimulationName().equals(key)){
                return simConf;
            }
        }
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

        Map<String, List<Map<String, Long>>> maps = simulationReport.getTotalNodeInfo();
        Set<String> keys = maps.keySet();
        for(String key : keys){
            List<Map<String, Long>> nodeMeans = maps.get(key);
            SimulationConfig simConf = getSimConfig(simulationReport.getSimulationConfig(), key);
            for(Map<String, Long> means : nodeMeans){
                Set<String> meanKey = means.keySet();
                for(String k : meanKey){
                    if (k.equals("agent")){
                        numerator += (simConf.getNumberInstances() * simConf.getNumberRepetitions()) *  means.get(k);
                    }
                    else if (k.equals("catalog")){
                        catalogNumerator += (simConf.getNumberInstances() * simConf.getNumberRepetitions()) *  means.get(k);
                    }
                    else if (k.equals("report")){
                        reportNumerator += (simConf.getNumberInstances() * simConf.getNumberRepetitions()) *  means.get(k);
                    }
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
    private List<SimulationConfig> getGatlingSimData(FilePath workspace, String simID) throws IOException {
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