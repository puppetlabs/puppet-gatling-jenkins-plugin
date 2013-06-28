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
package org.jenkinsci.plugins.customartifactbuilder.chart;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.customartifactbuilder.gatling.RequestReport;
import org.jenkinsci.plugins.customartifactbuilder.gatling.CustomBuildAction;

import com.excilys.ebi.gatling.jenkins.chart.Serie;
import com.excilys.ebi.gatling.jenkins.chart.SerieName;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class Graph<Y extends Number> {
	private static final Logger LOGGER = Logger.getLogger(Graph.class.getName());

	private final SortedMap<SerieName, Serie<Integer, Y>> series = new TreeMap<SerieName, Serie<Integer, Y>>();

	private final ObjectMapper mapper = new ObjectMapper();

	public Graph(AbstractProject<?, ?> project, int maxBuildsToDisplay) {
		int numberOfBuild = 0;
		
		for (AbstractBuild<?, ?> build : project.getBuilds()) {
			CustomBuildAction action = build.getAction(CustomBuildAction.class);
			
			if (action != null){
				numberOfBuild++;
				List<RequestReport> tmpList = action.getRequestReportList();
				int x = 0;
				for (RequestReport requestR : tmpList){
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

	protected abstract Y getValue(RequestReport requestReport);
}
