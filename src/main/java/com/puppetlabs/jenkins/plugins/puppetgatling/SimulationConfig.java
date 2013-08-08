package com.puppetlabs.jenkins.plugins.puppetgatling;

/**
 * SimulationConfig
 *
 * A quick snapshot of what a individual node simulation is configured to.
 *
 * @author Brian Cain
 */
public class SimulationConfig {
    private String simulationName;
    private int numberInstances;
    private int numberRepetitions;

    public SimulationConfig(String simulationName, int numberInstances, int numberRepetitions){
        this.simulationName = simulationName;
        this.numberInstances = numberInstances;
        this.numberRepetitions = numberRepetitions;
    }

    public String getSimulationName(){
        return simulationName;
    }

    public int getNumberInstances(){
        return numberInstances;
    }

    public int getNumberRepetitions(){
        return numberRepetitions;
    }
}
