package com.puppetlabs.jenkins.plugins.puppetgatling.chart;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.jenkins.chart.Point;
import io.gatling.jenkins.chart.Serie;
import io.gatling.jenkins.chart.SerieName;

import java.util.List;
import java.util.logging.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class RawDataGraph<Y extends Number> implements Graph<Y> {
    private static final Logger LOGGER = Logger.getLogger(SimulationGraph.class.getName());

    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<SerieName, Serie<Integer, Y>> series;

    public static <Y extends Number> Serie<Integer, Y>
        filterDataToSeries(List<Point<Integer, Y>> data, int maxDataPoints) {
        if (data.size() < maxDataPoints) {
            maxDataPoints = data.size();
        }

        Serie<Integer, Y> filteredSeries = new Serie<>();
        int filterStep = 1;
        if (data.size() > maxDataPoints) {
            filterStep = data.size() / maxDataPoints;
        }

        for (int i = 0; i < maxDataPoints; i ++) {
            int index = i * filterStep;
            Point<Integer, Y> point = data.get(index);
            filteredSeries.addPoint(point.getX(), point.getY());
        }

        return filteredSeries;
    }


    public RawDataGraph(Map<SerieName, Serie<Integer, Y>> series) {
        this.series = series;
    }

    // TODO: push up into base class
    @Override
    public String getSeriesNamesJSON() {
        String json = null;

        try {
            json = mapper.writeValueAsString(series.keySet());
        } catch (IOException e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
        }
        return json;
    }

    @Override
    public String getSeriesJSON() {
        String json = null;


        try {
            json = mapper.writeValueAsString(series.values());
        } catch (IOException e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
        }
        return json;
    }
}
