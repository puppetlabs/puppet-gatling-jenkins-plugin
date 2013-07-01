package com.puppetlabs.jenkins.plugins.puppetgatlingjenkinsplugin.gatling;

import hudson.model.Action;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class CustomProjectAction implements Action {

	private final AbstractProject<?, ?> project;
	
	public CustomProjectAction(AbstractProject<?, ?> project){
		this.project = project;
	}
	
	public AbstractProject<?, ?> getProject(){
		return project;
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
	
	public boolean isVisibule() {
		for (AbstractBuild<?, ?> build: getProject().getBuilds()){
			CustomBuildAction customBuildAction = build.getAction(CustomBuildAction.class);
			if (customBuildAction != null){
				return true;
			}
		}
		return false;
	}

}
