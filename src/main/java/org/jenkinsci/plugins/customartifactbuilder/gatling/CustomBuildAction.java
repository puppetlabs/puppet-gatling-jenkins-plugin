package org.jenkinsci.plugins.customartifactbuilder.gatling;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.AbstractBuild;

public class CustomBuildAction implements Action {
	
	private final AbstractBuild<?, ?> build;
	private final List<RequestReport> requestReportList;
	
	public CustomBuildAction(AbstractBuild<?, ?> build, List<RequestReport> requestReportList){
		this.build = build;
		this.requestReportList = requestReportList;
	}
	
	public AbstractBuild<?, ?> getbuild(){
		return build;
	}

	public List<RequestReport> getRequestReportList(){
		return requestReportList;
	}
	
	public String getIconFileName() {
		return "/plugin/customartifactbuilder/img/puppet.png";
	}

	public String getDisplayName() {
		return "Custom Gatling";
	}

	public String getUrlName() {
		return "cgatling";
	}
}