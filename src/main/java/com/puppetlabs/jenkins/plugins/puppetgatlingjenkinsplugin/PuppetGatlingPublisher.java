package com.puppetlabs.jenkins.plugins.puppetgatlingjenkinsplugin;

import static com.puppetlabs.jenkins.plugins.puppetgatlingjenkinsplugin.Constants.Constant.*;
import com.excilys.ebi.gatling.jenkins.BuildSimulation;
import com.excilys.ebi.gatling.jenkins.GatlingBuildAction;
import com.puppetlabs.jenkins.plugins.puppetgatlingjenkinsplugin.gatling.PuppetGatlingBuildAction;
import com.puppetlabs.jenkins.plugins.puppetgatlingjenkinsplugin.gatling.SimulationReport;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

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
import hudson.util.FormValidation;

/*
 * PuppetGatlingPublisher
 * 
 * This is where the majority of the logic resides for this plugin.
 * When added as a "post-build step" on a given job, perform() is called.
 * It then grabs all of the GatingBuildAction objects within build which 
 * is then extracted into a local GatlingBuildAction object. Then we 
 * iterate over all the available reports from the GatlingBuildAction object
 * to obtain and parse the stats.tsv file within each report. Once it's parsed,
 * the new calculations are made for Mean Agent Run Time and Mean Catalog
 * Compile Time. It is then added as our own Puppet Gatling Build action artifact.
 * 
 * Related .jelly file
 * 	- config.jelly
 * 
 * config.jelly
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
	
	@Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		logger = listener.getLogger();
		logger.println("[CustomArtifactDeployer] - Starting deployment from the post-action ...");
		
		boolean succ = getBuildAction(build, launcher, listener);
		
		if (!succ){
			logger.println("[CustomArtifactDeployer] - Get Build Action failed.");
			return succ;
		}
		
		return succ;
    }
	
	private boolean getBuildAction(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException{
		List<GatlingBuildAction> gba_lst = build.getActions(GatlingBuildAction.class);
		
		if (gba_lst.size() == 0){
			return false;
		}
		GatlingBuildAction action = gba_lst.get(0);
		int action_lst_size = action.getSimulations().size();
		
		
		List<SimulationReport> rrList = new ArrayList<SimulationReport>();
		int simulationCounter = 0;
		for (BuildSimulation sim : action.getSimulations()){
			FilePath simdir = action.getSimulations().get(simulationCounter).getSimulationDirectory();
			String stats_file_contents_path = simdir + "/stats.tsv";
			
			logger.println("[CustomArtifactDeployer] - The simulation directory is: " + simdir);
			logger.println("[CustomArtifactDeployer] - The stats file contents path is: " + stats_file_contents_path);
			
			List<Integer> calcList = getCalculations(stats_file_contents_path);
			
			SimulationReport requestReport = new SimulationReport();
			requestReport.setMeanAgentRunTime(calcList.get(2).longValue());
			requestReport.setMeanCatalogCompileTime(calcList.get(1).longValue());
			requestReport.setName(sim.getSimulationName());
			simulationCounter++;
			rrList.add(requestReport);
		}
		
		PuppetGatlingBuildAction customAction = new PuppetGatlingBuildAction(build, rrList);
		build.addAction(customAction);
		return true;
	}
	
	private List<Integer> getCalculations(String statsFilePath) throws IOException{
		List<Integer> calcList = new ArrayList<Integer>();

		LineIterator it = FileUtils.lineIterator(new File(statsFilePath));

		int globTotal = 0;
		int catMean = 0;
		int totalMean = 0;

		try{
			while(it.hasNext()){
				String line = it.nextLine();
				String[] tmp_toke = line.split("\t");
				if (tmp_toke[0].equals("Global Information")){
					globTotal = Integer.parseInt(tmp_toke[1]);
				}
				else if (tmp_toke[0].equals("catalog")){
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

		calcList.add(globTotal);
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
    public static final class CustomArtifactDeployerDescriptor extends BuildStepDescriptor<Publisher> {
		
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public String getDisplayName() {
			// TODO Auto-generated method stub
			return DISPLAY_NAME;
		}
	}
}