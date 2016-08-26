package com.puppetlabs.jenkins.plugins.puppetgatling;

import static com.puppetlabs.jenkins.plugins.puppetgatling.Constant.*;

import com.puppetlabs.jenkins.plugins.puppetgatling.chart.Graph;
import com.puppetlabs.jenkins.plugins.puppetgatling.chart.SimulationGraph;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import java.util.*;

import com.puppetlabs.jenkins.plugins.puppetgatling.gatling.*;

/**
 * <h2>Puppet Gatling Project Action</h2>
 * <br>
 * This file relates to the three jelly files
 *
 * 	<ul>
 * 	    <li>floatingBox.jelly</li>
 * 	    <li>index.jelly</li>
 * 	</ul>
 * 
 * <h3>floatingBox.jelly</h3>
 *  <br>
 * 	This jelly file is responsible for the side panel on any given job in Jenkins with Puppet-Gating 
 * 	installed as a plugin and added as a post build. If a build is available, it will display a 
 * 	graph on the side with Mean Agent Run Time. The jelly file calls the function below to 
 *  obtain that graph.
 *  
 * <h3>index.jelly</h3>
 * <br>
 * 	This jelly file is what will be displayed when you click on "Puppet Gatling" on the main job page.
 *  It obtains graphs similar to floatingBox.jelly, with the functions below.
 * 
 * @author Brian Cain
 */
public class PuppetGatlingProjectAction implements Action {

    private final Job<?, ?> job;

    public PuppetGatlingProjectAction(Job<?, ?> job) {
        this.job = job;
    }

    private Run getLastSuccessfulBuild() {
        return job.getLastSuccessfulBuild();
    }
    
    public Job<?, ?> getJob() {
    	return job;
    }

    public boolean isVisible() {
		for (Run<?, ?> run: getJob().getBuilds()) {
			PuppetGatlingBuildAction gatlingBuildAction = run.getAction(PuppetGatlingBuildAction.class);
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
    
    public Graph<Long> getDashboardGraph() {
    	return new SimulationGraph<Long>(job, MAX_BUILDS_TO_DISPLAY_DASHBOARD) {
			@Override
			public Long getValue(SimulationReport requestReport) {
				return requestReport.getTotalMeanAgentRunTime();
			}
		};
    }
    
    public Graph<Long> getAgentRunTime(){
    	return new SimulationGraph<Long>(job, MAX_BUILDS_TO_DISPLAY_DASHBOARD) {
			@Override
			public Long getValue(SimulationReport requestReport) {
				return requestReport.getTotalMeanAgentRunTime();
			}
		};
    }
    
    public Graph<Long> getCatalogCompileTime(){
    	return new SimulationGraph<Long>(job, MAX_BUILDS_TO_DISPLAY_DASHBOARD) {
			@Override
			public Long getValue(SimulationReport requestReport) {
				return requestReport.getTotalMeanCatalogResponseTime();
			}
		};
    }

    public Graph<Long> getReportRequestTime(){
        return new SimulationGraph<Long>(job, MAX_BUILDS_TO_DISPLAY_DASHBOARD) {
            @Override
            public Long getValue(SimulationReport requestReport) {
                return requestReport.getTotalReportResponseTime();
            }
        };
    }

    public Graph<Long> getFailedRequests(){
        return new SimulationGraph<Long>(job, MAX_BUILDS_TO_DISPLAY_DASHBOARD) {
            @Override
            public Long getValue(SimulationReport requestReport) {
                return (long) requestReport.getTotalFailedRequests();
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

    public Map<Run<?, ?>, List<String>> getReports() {
        Map<Run<?, ?>, List<String>> reports = new LinkedHashMap<Run<?, ?>, List<String>>();

        for (Run<?, ?> run : job.getBuilds()) {
            PuppetGatlingBuildAction action = run.getAction(PuppetGatlingBuildAction.class);
            if (action != null) {
                List<String> simNames = new ArrayList<String>();
                for (SimulationReport sim : action.getSimulationReportList()) {
                    simNames.add(sim.getName());
                }
                reports.put(run, simNames);
            }
        }

        return reports;
    }

    public String getReportURL(int build, String simName) {
        return new StringBuilder().append(build).append("/").append("gatling").append("/report/").append(simName).toString();
    }
}
