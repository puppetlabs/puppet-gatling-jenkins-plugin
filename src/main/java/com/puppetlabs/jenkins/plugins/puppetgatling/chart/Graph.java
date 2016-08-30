package com.puppetlabs.jenkins.plugins.puppetgatling.chart;

public interface Graph<Y extends Number> {
    public String getSeriesNamesJSON();
    public String getSeriesJSON();
}
