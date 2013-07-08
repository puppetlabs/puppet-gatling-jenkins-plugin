package com.puppetlabs.jenkins.plugins.puppetgatling;

import static com.puppetlabs.jenkins.plugins.puppetgatling.Constant.*;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Run;

import com.puppetlabs.jenkins.plugins.puppetgatling.chart.Graph;
import com.puppetlabs.jenkins.plugins.puppetgatling.gatling.*;

/**
 * <h2>Puppet Gatling Project Action</h2>
 * <br></br>
 * This file relates to the three jelly files
 *
 * 	<ul>
 * 	    <li>floatingBox.jelly</li>
 * 	    <li>index.jelly</li>
 * 	</ul>
 * 
 * <h3>floatingBox.jelly</h3>
 *  <br></br>
 * 	This jelly file is responsible for the side panel on any given job in Jenkins with Puppet-Gating 
 * 	installed as a plugin and added as a post build. If a build is available, it will display a 
 * 	graph on the side with Mean Agent Run Time. The jelly file calls the function below to 
 *  obtain that graph.
 *  
 * <h3>index.jelly</h3>
 * <br></br>
 * 	This jelly file is what will be displayed when you click on "Puppet Gatling" on the main job page.
 *  It obtains graphs similar to floatingBox.jelly, with the functions below.
 * 
 * @author Brian Cain
 */
public class PuppetGatlingProjectAction implements Action {

    private final AbstractProject<?, ?> project;

    public PuppetGatlingProjectAction(AbstractProject<?, ?> project) {
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
			PuppetGatlingBuildAction gatlingBuildAction = build.getAction(PuppetGatlingBuildAction.class);
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
			public Long getValue(SimulationReport requestReport) {
				return requestReport.getMeanAgentRunTime();
			}
		};
    }
    
    public Graph<Long> getagentRunTime(){
    	return new Graph<Long>(project, MAX_BUILDS_TO_DISPLAY_DASHBOARD) {
			@Override
			public Long getValue(SimulationReport requestReport) {
				return requestReport.getMeanAgentRunTime();
			}
		};
    }
    
    public Graph<Long> getcatalogCompileTime(){
    	return new Graph<Long>(project, MAX_BUILDS_TO_DISPLAY_DASHBOARD) {
			@Override
			public Long getValue(SimulationReport requestReport) {
				return requestReport.getMeanCatalogCompileTime();
			}
		};
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
