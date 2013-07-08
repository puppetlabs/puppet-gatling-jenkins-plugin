package com.puppetlabs.jenkins.plugins.puppetgatling.gatling;

import static com.puppetlabs.jenkins.plugins.puppetgatling.Constant.*;

import java.util.List;

import hudson.model.Action;
import hudson.model.AbstractBuild;

/**
 * Puppet Gatling Build Action
 *
 * @author Brian Cain
 */
public class PuppetGatlingBuildAction implements Action {
	
	private final AbstractBuild<?, ?> build;
	private final List<SimulationReport> requestReportList;
	
	public PuppetGatlingBuildAction(AbstractBuild<?, ?> build, List<SimulationReport> requestReportList){
		this.build = build;
		this.requestReportList = requestReportList;
	}
	
	public AbstractBuild<?, ?> getbuild(){
		return build;
	}

	public List<SimulationReport> getRequestReportList(){
		return requestReportList;
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