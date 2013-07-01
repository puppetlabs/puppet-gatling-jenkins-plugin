package com.puppetlabs.jenkins.plugins.puppetgatlingjenkinsplugin;

import static com.excilys.ebi.gatling.jenkins.PluginConstants.MAX_BUILDS_TO_DISPLAY_DASHBOARD;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Run;

import java.util.List;


import com.excilys.ebi.gatling.jenkins.GatlingBuildAction;
import com.puppetlabs.jenkins.plugins.puppetgatlingjenkinsplugin.chart.Graph;
import com.puppetlabs.jenkins.plugins.puppetgatlingjenkinsplugin.gatling.RequestReport;

/**
 * @author Gregory Boissinot
 * Modifications by Brian Cain
 */
public class ArtifactDeployerProjectAction implements Action {

    private final AbstractProject<?, ?> project;

    public ArtifactDeployerProjectAction(AbstractProject<?, ?> project) {
        this.project = project;
    }

    private Run getLastSuccessfulBuild() {
        return project.getLastSuccessfulBuild();
    }
    
    public AbstractProject<?, ?> getProject() {
    	return project;
    }
    
    public boolean isVisible() {
		for (AbstractBuild<?, ?> build : getProject().getBuilds()) {
			GatlingBuildAction gatlingBuildAction = build.getAction(GatlingBuildAction.class);
			if (gatlingBuildAction != null) {
				return true;
			}
		}
		return false;
	}

    @SuppressWarnings("unused")
    public int getLastSuccessfulNumber() {
        Run latestSuccessfulBuild = getLastSuccessfulBuild();
        if (latestSuccessfulBuild == null) {
            return 0;
        }
        return latestSuccessfulBuild.getNumber();
    }
    
    public Graph<Long> getdashboardGraph() {
    	return new Graph<Long>(project, MAX_BUILDS_TO_DISPLAY_DASHBOARD) {
			@Override
			public Long getValue(RequestReport requestReport) {
				return requestReport.getMeanAgentRunTime();
			}
		};
    }
    
    public Graph<Long> getagentRunTime(){
    	return new Graph<Long>(project, MAX_BUILDS_TO_DISPLAY_DASHBOARD) {
			@Override
			public Long getValue(RequestReport requestReport) {
				return requestReport.getMeanAgentRunTime();
			}
		};
    }
    
    public Graph<Long> getcatalogCompileTime(){
    	return new Graph<Long>(project, MAX_BUILDS_TO_DISPLAY_DASHBOARD) {
			@Override
			public Long getValue(RequestReport requestReport) {
				return requestReport.getMeanCatalogCompileTime();
			}
		};
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
