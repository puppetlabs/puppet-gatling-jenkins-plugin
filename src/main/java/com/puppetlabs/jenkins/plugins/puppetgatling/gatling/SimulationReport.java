package com.puppetlabs.jenkins.plugins.puppetgatling.gatling;

import com.puppetlabs.jenkins.plugins.puppetgatling.*;
import java.util.*;

/**
 * SimulationReport
 * <br></br>
 * A given simulation report for Puppet Gatling
 * 
 * @author Brian Cain
 */
public class SimulationReport {

	private String name;
    private String speedOfCPU;
    private String numCPUs;
    private String puppetVersion;
    private String diskSizeBytes;
    private String memSize;
    private String puppetAcceptanceSHA;
    private String gatlingPuppetLoadTestSHA;
    private String facterData;

    private List<SimulationConfig> simulationConfig;
    private Long totalMeanAgentRunTime;
    private int totalFailedRequests;
    private Long totalMeanCatalogResponseTime;
    private Long totalReportResponseTime;
    private Map<String, List<SimulationData>> simulationDataList;

    private Map<String, List<Map<String, Long>>> totalNodeInfo;

    public Long getOtherResponseTime(){
        return (this.totalMeanAgentRunTime - (this.totalMeanCatalogResponseTime + this.totalReportResponseTime));
    }
	
	public String getName(){
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}

    public String getSpeedOfCPU(){
        return speedOfCPU;
    }

    public void setSpeedOfCPU(String speedOfCPU){
        this.speedOfCPU = speedOfCPU;
    }

    public String getNumCPUs(){
        return numCPUs;
    }

    public void setNumCPUs(String numCPUs){
        this.numCPUs = numCPUs;
    }

    public String getPuppetVersion(){
        return puppetVersion;
    }

    public void setPuppetVersion(String puppetVersion){
        this.puppetVersion = puppetVersion;
    }

    public String getDiskSizeBytes(){
        return diskSizeBytes;
    }

    public void setDiskSizeBytes(String diskSizeBytes){
        this.diskSizeBytes = diskSizeBytes;
    }

    public String getMemSize(){
        return memSize;
    }

    public void setMemSize(String memSize){
        this.memSize = memSize;
    }

    public String getPuppetAcceptanceSHA(){
        return puppetAcceptanceSHA;
    }

    public void setPuppetAcceptanceSHA(String puppetAcceptanceSHA){
        this.puppetAcceptanceSHA = puppetAcceptanceSHA;
    }

    public String getGatlingPuppetLoadTestSHA(){
        return gatlingPuppetLoadTestSHA;
    }

    public void setGatlingPuppetLoadTestSHA(String gatlingPuppetLoadTestSHA){
        this.gatlingPuppetLoadTestSHA = gatlingPuppetLoadTestSHA;
    }

    public String getFacterData(){
        return facterData;
    }

    public void setFacterData(String facterData){
        this.facterData = facterData;
    }

    public List<SimulationConfig> getSimulationConfig(){
        return simulationConfig;
    }

    public void setSimulationConfig(List<SimulationConfig> simulationConfig){
        this.simulationConfig = simulationConfig;
    }

    public Long getTotalMeanAgentRunTime(){
        return totalMeanAgentRunTime;
    }

    public void setTotalMeanAgentRunTime(Long totalMeanAgentRunTime){
        this.totalMeanAgentRunTime = totalMeanAgentRunTime;
    }

    public int getTotalFailedRequests(){
        return totalFailedRequests;
    }

    public void setTotalFailedRequests(int totalFailedRequests){
        this.totalFailedRequests = totalFailedRequests;
    }

    public Long getTotalMeanCatalogResponseTime(){
        return totalMeanCatalogResponseTime;
    }

    public void setTotalMeanCatalogResponseTime(Long totalMeanCatalogResponseTime){
        this.totalMeanCatalogResponseTime = totalMeanCatalogResponseTime;
    }

    public Long getTotalReportResponseTime(){
        return totalReportResponseTime;
    }

    public void setTotalReportResponseTime(Long totalReportResponseTime){
        this.totalReportResponseTime = totalReportResponseTime;
    }

    public Map<String, List<SimulationData>> getSimulationDataList(){
        return simulationDataList;
    }

    public void setSimulationDataList(Map<String, List<SimulationData>> simulationDataList){
        this.simulationDataList = simulationDataList;
    }

    public Map<String, List<Map<String, Long>>> getTotalNodeInfo(){
        return totalNodeInfo;
    }

    public void setTotalNodeInfo(Map<String, List<Map<String, Long>>> totalNodeInfo){
        this.totalNodeInfo = totalNodeInfo;
    }
	
}