package com.puppetlabs.jenkins.plugins.puppetgatling.gatling;

import static com.puppetlabs.jenkins.plugins.puppetgatling.Constant.*;

import java.util.*;

import hudson.model.Action;
import hudson.model.AbstractBuild;

/**
 * Puppet Gatling Build Action
 *
 * @author Brian Cain
 */
public class PuppetGatlingBuildAction implements Action {
	
	private final AbstractBuild<?, ?> build;
	private final List<SimulationReport> simulationReportList;
	
	public PuppetGatlingBuildAction(AbstractBuild<?, ?> build, List<SimulationReport> simulationReportList){
		this.build = build;
		this.simulationReportList = simulationReportList;
	}
	
	public AbstractBuild<?, ?> getbuild(){
		return build;
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

    public Long getCatalogResponseTime(String key, SimulationReport simulationReport){
        for(Map.Entry entry : simulationReport.getMeanCatalogResponseTimePerNode().entrySet()){
            if (entry.getKey().equals(key)){
                return Long.parseLong(entry.getValue().toString());
            }
        }
        return 0L;
    }

    public Long getReportResponseTime(String key, SimulationReport simulationReport){
        for(Map.Entry entry: simulationReport.getMeanReportResponseTimePerNode().entrySet()){
            if (entry.getKey().equals(key)){
                return Long.parseLong(entry.getKey().toString());
            }
        }
        return 0L;
    }


}