package com.puppetlabs.jenkins.plugins.puppetgatling;

/**
 * Constant
 * <br></br>
 * Location for important constant values related to the project
 * 
 * @author Brian Cain
 */

public class Constant {
	public static final String ICON_URL = "/plugin/puppet-gatling-jenkins-plugin/img/puppet.png";
    public static final String DISPLAY_NAME = "Puppet Gatling";
    public static final String URL_NAME = "puppet-gatling";

    public static final int MAX_BUILDS_TO_DISPLAY = 30;
    public static final int MAX_BUILDS_TO_DISPLAY_DASHBOARD = 15;

    public static final int GATLING_STATS_INDEX_GROUP_STAT = 0;
    public static final int GATLING_STATS_INDEX_TOTAL_REQUESTS = 1;
    public static final int GATLING_STATS_INDEX_SUCCESSFUL_REQUESTS = 2;
    public static final int GATLING_STATS_INDEX_FAILED_REQUESTS = 3;
    public static final int GATLING_STATS_INDEX_MEAN_RESPONSE_TIME = 10;
}