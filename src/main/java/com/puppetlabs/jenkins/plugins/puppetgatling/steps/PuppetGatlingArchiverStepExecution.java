package com.puppetlabs.jenkins.plugins.puppetgatling.steps;

import com.puppetlabs.jenkins.plugins.puppetgatling.PuppetGatlingPublisher;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

public class PuppetGatlingArchiverStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

    @StepContextParameter
    private transient Run build;

    @StepContextParameter
    private transient FilePath ws;

    @StepContextParameter
    private transient Launcher launcher;

    @StepContextParameter
    private transient TaskListener listener;

    @Override
    protected Void run() throws Exception {
        System.out.println("Running Puppet Gatling Archiver step");

        PuppetGatlingPublisher publisher = new PuppetGatlingPublisher(true);
        publisher.perform(build, ws, launcher, listener);

        return null;
    }

    private static final long serialVersionUID = 1L;
}
