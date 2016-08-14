package com.puppetlabs.jenkins.plugins.puppetgatling.gatling;

import static com.puppetlabs.jenkins.plugins.puppetgatling.Constant.*;

import java.util.*;

import hudson.model.Action;
import hudson.model.Run;

/**
 * Puppet Gatling Build Action
 *
 * @author Brian Cain
 */
public class PuppetGatlingBuildAction implements Action {
	
	private final Run<?, ?> run;
	private final List<SimulationReport> simulationReportList;
	
	public PuppetGatlingBuildAction(Run<?, ?> run, List<SimulationReport> simulationReportList){
		this.run = run;
		this.simulationReportList = simulationReportList;
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
}