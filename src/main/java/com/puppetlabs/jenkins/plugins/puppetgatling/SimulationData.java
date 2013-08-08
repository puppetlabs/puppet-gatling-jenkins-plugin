package com.puppetlabs.jenkins.plugins.puppetgatling;

/**
 * SimulationData
 *
 * This object was created to be used within a HashMap while parsing the gatling reports, specifically stats.tsv
 *
 * A given statistic within a simulation.
 *
 * @author Brian Cain
 */
public class SimulationData {
    private String key;
    private String stat;
    private int totalRequests;
    private int successfulRequests;
    private int failedRequests;
    private int meanResponseTime;

    public SimulationData(String key, String stat, int totalRequests, int successfulRequests, int failedRequests, int meanResponseTime){
        this.key = key;
        this.stat = stat;
        this.totalRequests = totalRequests;
        this.successfulRequests = successfulRequests;
        this.failedRequests = failedRequests;
        this.meanResponseTime = meanResponseTime;
    }

    public String getKey(){
        return key;
    }

    public String getStat(){
        return stat;
    }

    public int getTotalRequests(){
        return totalRequests;
    }

    public int getSuccessfulRequests(){
        return successfulRequests;
    }

    public int getFailedRequests(){
        return failedRequests;
    }

    public int getMeanResponseTime(){
        return meanResponseTime;
    }

    /**
     * prettyPrint - Prints the object so it doesn't make your eyes bleed
     *
     * @return result - String to print out
     */
    public String prettyPrint(){
        String whitespace = "";
        int needed = 39-this.stat.length();

        for(int i = 0; i<= needed + 3; i++){
            whitespace += " ";
        }

        String result = this.key + " " + this.stat + whitespace + "[" + this.totalRequests + ",    " + this.successfulRequests + ",    " + this.failedRequests + ",    " +
                this.meanResponseTime + "]";
        return result;
    }
}
