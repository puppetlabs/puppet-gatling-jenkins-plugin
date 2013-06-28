package org.jenkinsci.plugins.customartifactbuilder.gatling;

import hudson.model.DirectoryBrowserSupport;
import org.kohsuke.stapler.ForwardToView;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * This class is used by the {@link GatlingBuildAction} to handle the rendering
 * of gatling reports.
 */
public class ReportRenderer {

    private CustomBuildAction action;
    private BuildSimulation simulation;

    public ReportRenderer(CustomBuildAction gatlingBuildAction, BuildSimulation simulation) {
        this.action = gatlingBuildAction;
        this.simulation = simulation;
    }

    /**
     * This method will be called when there are no remaining URL tokens to
     * process after {@link GatlingBuildAction} has handled the initial
     * `/report/MySimulationName` prefix.  It renders the `report.jelly`
     * template inside of the Jenkins UI.
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
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
     * @param request
     * @param response
     * @throws IOException
     * @throws ServletException
     */
    public void doSource(StaplerRequest request, StaplerResponse response)
            throws IOException, ServletException {
        DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(action,
                simulation.getSimulationDirectory(),
                simulation.getSimulationName(), null, false);
        dbs.generateResponse(request, response, action);
    }
}