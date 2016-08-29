package com.puppetlabs.jenkins.plugins.puppetgatling.gatling;

import static com.puppetlabs.jenkins.plugins.puppetgatling.Constant.*;

import java.util.*;

import com.puppetlabs.jenkins.plugins.puppetgatling.PuppetGatlingProjectAction;
import com.puppetlabs.jenkins.plugins.puppetgatling.chart.Graph;
import com.puppetlabs.jenkins.plugins.puppetgatling.chart.RawDataGraph;
import hudson.model.Action;
import hudson.model.Run;
import io.gatling.jenkins.BuildSimulation;
import io.gatling.jenkins.chart.Point;
import io.gatling.jenkins.chart.Serie;
import io.gatling.jenkins.chart.SerieName;
import jenkins.tasks.SimpleBuildStep;

/**
 * Puppet Gatling Build Action
 *
 * @author Brian Cain
 */
public class PuppetGatlingBuildAction implements Action, SimpleBuildStep.LastBuildAction {

	private static final int MAX_MEMORY_DATA_POINTS_TO_DISPLAY = 40;

	private final Run<?, ?> run;
	private final List<SimulationReport> simulationReportList;
	private final List<BuildSimulation> sims;

	public PuppetGatlingBuildAction(Run<?, ?> run, List<BuildSimulation> sims,
									List<SimulationReport> simulationReportList){
		this.run = run;
		this.sims = sims;
		this.simulationReportList = simulationReportList;
	}
	
	public Run<?, ?> getRun(){
		return run;
	}

	public List<BuildSimulation> getSimulations() {
		return sims;
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

	/**
	 * This method is called dynamically for any HTTP request to our plugin's
	 * URL followed by "/report/SomeSimulationName".
	 *
	 * It returns a new instance of {@link ReportRenderer}, which contains the
	 * actual logic for rendering a report.
	 *
	 * @param simulationName the name of the simulation
	 * @return a renderer for the report by the given name
	 */
	public ReportRenderer getReport(String simulationName) {
		return new ReportRenderer(this, getSimulation(simulationName));
	}

	public String getReportURL(String simulationName) {
		return new StringBuilder().append("job/").
				append(run.getParent().getName()).
				append("/").
				append(run.getNumber()).
				append("/").
				append(URL_NAME).
				append("/report/").
				append(simulationName).
				toString();
	}

	public Graph<Long> getMemoryUsage() {
		if (false) {
			List<Point<Integer, Long>> memoryData = new ArrayList<>();
			SerieName memSeriesName = new SerieName("memory");
			for (int i = 0; i < 1000; i++) {
				memoryData.add(new Point<Integer, Long>(i, (long) (2000 + (-100 + (Math.random() * 200)))));
			}
			Map<SerieName, Serie<Integer, Long>> fakeData = new TreeMap<>();
			fakeData.put(memSeriesName, RawDataGraph.filterDataToSeries(memoryData, MAX_MEMORY_DATA_POINTS_TO_DISPLAY));

			return new RawDataGraph<Long>(fakeData);
		} else {
			return null;
		}
	}

	private BuildSimulation getSimulation(String simulationName) {
		// this isn't the most efficient implementation in the world :)
		for (BuildSimulation sim : this.getSimulations()) {
			if (sim.getSimulationName().equals(simulationName)) {
				return sim;
			}
		}
		return null;
	}

	@Override
	public Collection<? extends Action> getProjectActions() {
		List<PuppetGatlingProjectAction> projectActions = new ArrayList<>();
		projectActions.add(new PuppetGatlingProjectAction(run.getParent()));
		return projectActions;
	}
}
