package com.puppetlabs.jenkins.plugins.puppetgatling;

import static com.puppetlabs.jenkins.plugins.puppetgatling.Constant.*;
import com.excilys.ebi.gatling.jenkins.BuildSimulation;
import com.excilys.ebi.gatling.jenkins.GatlingBuildAction;
import com.puppetlabs.jenkins.plugins.puppetgatling.gatling.PuppetGatlingBuildAction;
import com.puppetlabs.jenkins.plugins.puppetgatling.gatling.SimulationReport;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.*;

import org.apache.commons.io.FileUtils;
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
		int simulationCounter = 0;
		for (BuildSimulation sim : action.getSimulations()){
			FilePath simdir = action.getSimulations().get(simulationCounter).getSimulationDirectory();
			String stats_file_contents_path = simdir + "/stats.tsv";
			
			logger.println("[PuppetGatling] - The simulation directory is: " + simdir);
			logger.println("[PuppetGatling] - The stats file contents path is: " + stats_file_contents_path);
			
			List<Integer> calcList = getCalculations(stats_file_contents_path);
			
			SimulationReport requestReport = new SimulationReport();

            logger.println("[PuppetGatling] - Mean Agent Run Time: " + calcList.get(1));
            logger.println("[PuppetGatling] - Mean Catalog Compile Time: " + calcList.get(0));
			requestReport.setMeanAgentRunTime(calcList.get(1).longValue());
			requestReport.setMeanCatalogCompileTime(calcList.get(0).longValue());
			requestReport.setName(sim.getSimulationName());
			simulationCounter++;
			simulationReportList.add(requestReport);
		}
		
		PuppetGatlingBuildAction customAction = new PuppetGatlingBuildAction(build, simulationReportList);
		build.addAction(customAction);
		return true;
	}

    /**
     * Parses stats.tsv file for values:
     *
     * <ul>
     *     <li>Mean Agent Run Time (a sum of all means except Global)</li>
     *     <li>Mean Catalog Compile Time</li>
     * </ul>
     * @param statsFilePath
     * @return {@code calcList} - A list of key integers needed to be added to a given SimulationReport
     * @throws IOException
     */
	private List<Integer> getCalculations(String statsFilePath) throws IOException{
		List<Integer> calcList = new ArrayList<Integer>();

		LineIterator it = FileUtils.lineIterator(new File(statsFilePath));

		int catMean = 0;
		int totalMean = 0;

		try{
			while(it.hasNext()){
				String line = it.nextLine();
				String[] tmp_toke = line.split("\t");
                if (tmp_toke[0].equals("catalog")){
					catMean = Integer.parseInt(tmp_toke[12]);
					totalMean += Integer.parseInt(tmp_toke[12]);
				}
				else if(tmp_toke.length > 1 && !tmp_toke[0].equals("name")){
					totalMean += Integer.parseInt(tmp_toke[12]);
				}
			}
		} finally{
			it.close();
		}

		calcList.add(catMean);
		calcList.add(totalMean);
		return calcList;
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