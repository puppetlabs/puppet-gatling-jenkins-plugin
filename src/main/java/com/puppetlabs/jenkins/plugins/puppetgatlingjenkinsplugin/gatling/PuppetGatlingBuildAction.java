package com.puppetlabs.jenkins.plugins.puppetgatlingjenkinsplugin.gatling;

import static com.puppetlabs.jenkins.plugins.puppetgatlingjenkinsplugin.Constants.Constant.*;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.AbstractBuild;

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