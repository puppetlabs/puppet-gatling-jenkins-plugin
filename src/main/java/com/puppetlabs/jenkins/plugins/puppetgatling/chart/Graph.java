/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.puppetlabs.jenkins.plugins.puppetgatling.chart;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import java.io.IOException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.excilys.ebi.gatling.jenkins.chart.Serie;
import com.excilys.ebi.gatling.jenkins.chart.SerieName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puppetlabs.jenkins.plugins.puppetgatling.gatling.PuppetGatlingBuildAction;
import com.puppetlabs.jenkins.plugins.puppetgatling.gatling.SimulationReport;

/**
 * Graph class
 * <br></br>
 * This is what the plugin uses to generate graphs. The graph constructor goes through each
 * Puppet Gatling build action to get a report. It then gets the report name and the data value
 * and places it as a point on the given graph.
 *
 * @author Brian Cain
 * @param <Y>
 */
public abstract class Graph<Y extends Number> {
	private static final Logger LOGGER = Logger.getLogger(Graph.class.getName());

	private final SortedMap<SerieName, Serie<Integer, Y>> series = new TreeMap<SerieName, Serie<Integer, Y>>();

	private final ObjectMapper mapper = new ObjectMapper();

	public Graph(AbstractProject<?, ?> project, int maxBuildsToDisplay) {
		int numberOfBuild = 0;
		
		for (AbstractBuild<?, ?> build : project.getBuilds()) {
			PuppetGatlingBuildAction action = build.getAction(PuppetGatlingBuildAction.class);
			
			if (action != null){
				numberOfBuild++;
				List<SimulationReport> tmpList = action.getSimulationReportList();
				for (SimulationReport requestR : tmpList){
					SerieName name = new SerieName(requestR.getName());
					if (!series.containsKey(name))
					    series.put(name, new Serie<Integer, Y>());
					
					series.get(name).addPoint(build.getNumber(), getValue(requestR));
				}
			}
			if (numberOfBuild >= maxBuildsToDisplay)
				break;
		}
	}

	public String getSeriesNamesJSON() {
		String json = null;

		try {
			json = mapper.writeValueAsString(series.keySet());
		} catch (IOException e) {
			LOGGER.log(Level.INFO, e.getMessage(), e);
		}
		return json;
	}

	public String getSeriesJSON() {
		String json = null;

		try {
			json = mapper.writeValueAsString(series.values());
		} catch (IOException e) {
			LOGGER.log(Level.INFO, e.getMessage(), e);
		}
		return json;
	}

	protected abstract Y getValue(SimulationReport requestReport);
}
