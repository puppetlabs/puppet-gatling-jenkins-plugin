package com.puppetlabs.jenkins.plugins.puppetgatling.gatling;

import static com.puppetlabs.jenkins.plugins.puppetgatling.Constant.*;

import java.util.*;

import com.puppetlabs.jenkins.plugins.puppetgatling.PuppetGatlingProjectAction;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.tasks.SimpleBuildStep;

/**
 * Puppet Gatling Build Action
 *
 * @author Brian Cain
 */
public class PuppetGatlingBuildAction implements Action, SimpleBuildStep.LastBuildAction {
	
	private final Run<?, ?> run;
	private final List<SimulationReport> simulationReportList;
	private final List<PuppetGatlingProjectAction> projectActions;
	
	public PuppetGatlingBuildAction(Run<?, ?> run, List<SimulationReport> simulationReportList){
		this.run = run;
		this.simulationReportList = simulationReportList;

		List<PuppetGatlingProjectAction> projectActions = new ArrayList<>();
		projectActions.add(new PuppetGatlingProjectAction(run.getParent()));
		this.projectActions = projectActions;
	}
	
	public Run<?, ?> getRun(){
		return run;
	}

	public List<SimulationReport> getSimulationReportList(){
		return simulationReportList;
	}
	
	public String getIconFileName() {
		return ICON_URL;
	}

	public String getDisplayName() {
		return DISPLAY_NAME;
	}

	public String getUrlName() {
		return URL_NAME;
	}

	@Override
	public Collection<? extends Action> getProjectActions() {
		return projectActions;
	}
}