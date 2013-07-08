package com.puppetlabs.jenkins.plugins.puppetgatling.gatling;

/**
 * SimulationReport
 * <br></br>
 * A given simulation report for Puppet Gatling
 * 
 * @author Brian Cain
 */
public class SimulationReport {

	private Long meanAgentRunTime;
	private Long meanCatalogCompileTime;
	private String name;
	
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}

	public Long getMeanAgentRunTime() {
		return meanAgentRunTime;
	}

	public void setMeanAgentRunTime(Long meanAgentRunTime) {
		this.meanAgentRunTime = meanAgentRunTime;
	}
	
	public Long getMeanCatalogCompileTime() {
		return meanCatalogCompileTime;
	}

	public void setMeanCatalogCompileTime(Long meanCatalogCompileTime) {
		this.meanCatalogCompileTime = meanCatalogCompileTime;
	}
	
}