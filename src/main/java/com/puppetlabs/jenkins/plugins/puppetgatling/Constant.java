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

    public String getIconUrl(){
        return ICON_URL;
    }

    public String getDisplayName(){
        return DISPLAY_NAME;
    }

    public String getUrlName(){
        return URL_NAME;
    }

    public int getMaxBuildsToDisplay(){
        return MAX_BUILDS_TO_DISPLAY;
    }

    public int getMaxBuildsToDisplayDashboard(){
        return MAX_BUILDS_TO_DISPLAY_DASHBOARD;
    }
}