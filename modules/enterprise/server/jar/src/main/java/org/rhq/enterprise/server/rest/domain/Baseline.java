package org.rhq.enterprise.server.rest.domain;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * A baseline
 * @author Heiko W. Rupp
 */
@XmlRootElement
public class Baseline {

    double min;
    double max;
    double mean;
    long computeTime;

    public Baseline() {
    }

    public Baseline(double min, double max, double mean, long computeTime) {
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.computeTime = computeTime;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public long getComputeTime() {
        return computeTime;
    }

    public void setComputeTime(long computeTime) {
        this.computeTime = computeTime;
    }
}
