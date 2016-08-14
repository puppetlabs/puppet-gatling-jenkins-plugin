package com.puppetlabs.jenkins.plugins.puppetgatling.steps;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

public class PuppetGatlingArchiverStep extends AbstractStepImpl {
    @DataBoundConstructor
    public PuppetGatlingArchiverStep() {}

    @Extension
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {
        public DescriptorImpl() { super(PuppetGatlingArchiverStepExecution.class); }

        @Override
        public String getFunctionName() {
            return "puppetGatlingArchive";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Archive Puppet Gatling reports";
        }
    }
}