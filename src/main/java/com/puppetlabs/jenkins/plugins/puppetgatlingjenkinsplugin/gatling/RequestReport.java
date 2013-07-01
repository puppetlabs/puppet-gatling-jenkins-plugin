package com.puppetlabs.jenkins.plugins.puppetgatlingjenkinsplugin.gatling;

public class RequestReport {

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