package com.puppetlabs.jenkins.plugins.puppetgatling.gatling;

import java.util.List;

public class NodeSimulationData {
    private final SimulationData groupData;
    private final List<SimulationData> requestData;

    public NodeSimulationData(SimulationData groupData, List<SimulationData> requestData) {
        this.groupData = groupData;
        this.requestData = requestData;
    }

    public SimulationData getGroupData() {
        return groupData;
    }

    public List<SimulationData> getRequestData() {
        return requestData;
    }
}
