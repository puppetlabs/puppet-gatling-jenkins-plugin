package com.puppetlabs.jenkins.plugins.puppetgatling.steps;

import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;

public class PuppetGatlingArchiverStepExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {
    @Override
    protected Void run() throws Exception {
        System.out.println("Running Puppet Gatling Archiver step");
        return null;
    }
}
