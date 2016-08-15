package com.puppetlabs.jenkins.plugins.puppetgatling.gatling;

import hudson.FilePath;
import hudson.model.Run;
import io.gatling.jenkins.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NOTE: this class consists almost entirely of code copy/pasted out of GatlingPublisher,
 *  from the upstream gatling-plugin repo.  The reason for this is because we would
 *  like to be able to use their logic for archiving the report files, but use our
 *  own logic for registering build actions / GUI elements to render in Jenkins
 *  (because their default graph of 'mean request time' is not really all that useful
 *  for us), and the methods called here are private in the upstream.  We should file a
 *  PR upstream that adds this archiver as its own class that is consumed by the Publisher
 *  class - I'm pretty sure they'll accept a PR like that.
 */
public class GatlingReportArchiver {
    public  List<BuildSimulation> saveFullReports(Run<?, ?> build, PrintStream logger, FilePath workspace) throws IOException, InterruptedException {
        FilePath[] files = workspace.list("**/global_stats.json");
        List<FilePath> reportFolders = new ArrayList<FilePath>();

        if (files.length == 0) {
            logger.println("Could not find a Gatling report in results folder.");
            return Collections.emptyList();
        }

        // Get reports folders for all "global_stats.json" found
        for (FilePath file : files) {
            reportFolders.add(file.getParent().getParent());
        }

        List<FilePath> reportsToArchive = selectReports(build, logger, reportFolders);
        logger.println("Found " + reportsToArchive.size() + " reports to archive.");


        // If the most recent report has already been archived, there's nothing else to do
        if (reportsToArchive.isEmpty()) {
            return Collections.emptyList();
        }

        List<BuildSimulation> simsToArchive = new ArrayList<BuildSimulation>();

        File allSimulationsDirectory = new File(build.getRootDir(), "simulations");
        if (!allSimulationsDirectory.exists()) {
            boolean mkdirResult = allSimulationsDirectory.mkdir();
            if (! mkdirResult) {
                logger.println("Could not create simulations archive directory '" + allSimulationsDirectory + "'");
                return Collections.emptyList();
            }
        }

        for (FilePath reportToArchive : reportsToArchive) {
            String name = reportToArchive.getName();
            int dashIndex = name.lastIndexOf('-');
            String simulation = name.substring(0, dashIndex);
            File simulationDirectory = new File(allSimulationsDirectory, name);
            boolean mkdirResult = simulationDirectory.mkdir();
            if (! mkdirResult) {
                logger.println("Could not create simulation archive directory '" + simulationDirectory + "'");
                return Collections.emptyList();
            }

            FilePath reportDirectory = new FilePath(simulationDirectory);

            reportToArchive.copyRecursiveTo(reportDirectory);

            io.gatling.jenkins.SimulationReport report = new io.gatling.jenkins.SimulationReport(reportDirectory, simulation);
            report.readStatsFile();
            BuildSimulation sim = new BuildSimulation(simulation, report.getGlobalReport(), reportDirectory);

            simsToArchive.add(sim);
        }


        return simsToArchive;
    }

    private List<FilePath> selectReports(Run<?, ?> build, PrintStream logger, List<FilePath> reportFolders) throws InterruptedException, IOException {
        long buildStartTime = build.getStartTimeInMillis();
        List<FilePath> reportsFromThisBuild = new ArrayList<FilePath>();
        for (FilePath reportFolder : reportFolders) {
            long reportLastMod = reportFolder.lastModified();
            if (reportLastMod > buildStartTime) {
                logger.println("Adding report '" + reportFolder.getName() +
                        "' because mtime '" + reportLastMod +
                        "' is newer than the build start time '" + buildStartTime + "'");
                reportsFromThisBuild.add(reportFolder);
            } else {
                logger.println("Not adding report '" + reportFolder.getName() +
                        "' because mtime '" + reportLastMod +
                        "' is older than the build start time '" + buildStartTime + "'");
            }
        }
        return reportsFromThisBuild;
    }
}
