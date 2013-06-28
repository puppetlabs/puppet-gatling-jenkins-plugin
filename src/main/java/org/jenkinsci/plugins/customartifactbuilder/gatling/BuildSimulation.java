package org.jenkinsci.plugins.customartifactbuilder.gatling;

import hudson.FilePath;

// This file is not needed

/**
 * This class is basically just a struct to hold information about one
 * or more gatling simulations that were archived for a given
 * instance of {@link GatlingBuildAction}.
 */
public class BuildSimulation {
    private final String simulationName;
    private final RequestReport requestReport;
    private final FilePath simulationDirectory;

    public BuildSimulation(String simulationName, RequestReport requestReport, FilePath simulationDirectory) {
        this.simulationName = simulationName;
        this.requestReport = requestReport;
        this.simulationDirectory = simulationDirectory;
    }

    public String getSimulationName() {
        return simulationName;
    }

    public RequestReport getRequestReport() {
        return requestReport;
    }

    public FilePath getSimulationDirectory() {
        return simulationDirectory;
    }
}
