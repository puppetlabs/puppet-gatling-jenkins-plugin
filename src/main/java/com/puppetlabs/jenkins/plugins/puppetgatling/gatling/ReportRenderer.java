package com.puppetlabs.jenkins.plugins.puppetgatling.gatling;

import hudson.model.Action;
import hudson.model.DirectoryBrowserSupport;
import io.gatling.jenkins.BuildSimulation;
import io.gatling.jenkins.GatlingBuildAction;
import org.kohsuke.stapler.ForwardToView;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * This class is used by the {@link PuppetGatlingBuildAction} to handle the rendering
 * of gatling reports.
 */
public class ReportRenderer {

  private Action action;
  private BuildSimulation simulation;

  public ReportRenderer(Action gatlingBuildAction, BuildSimulation simulation) {
    this.action = gatlingBuildAction;
    this.simulation = simulation;
  }

  /**
   * This method will be called when there are no remaining URL tokens to
   * process after {@link GatlingBuildAction} has handled the initial
   * `/report/MySimulationName` prefix.  It renders the `report.jelly`
   * template inside of the Jenkins UI.
   *
   * @param request the stapler request
   * @param response the stapler response that is being built
   * @throws IOException if an IO error occurs
   * @throws ServletException if a Servlet error occurs
   */
  public void doIndex(StaplerRequest request, StaplerResponse response)
    throws IOException, ServletException {
    ForwardToView forward = new ForwardToView(action, "report.jelly")
      .with("simName", simulation.getSimulationName());
    forward.generateResponse(request, response, action);
  }

  /**
   * This method will be called for all URLs that are routed here by
   * {@link GatlingBuildAction} with a prefix of `/source`.
   *
   * All such requests basically result in the servlet simply serving
   * up content files directly from the archived simulation directory
   * on disk.
   *
   * @param request the stapler request
   * @param response the stapler response that is being built
   * @throws IOException if an IO error occurs
   * @throws ServletException if a Servlet error occurs
   */
  public void doSource(StaplerRequest request, StaplerResponse response)
    throws IOException, ServletException {
    DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(action,
      simulation.getSimulationDirectory(),
      simulation.getSimulationName(), null, false);
    dbs.generateResponse(request, response, action);
  }
}
