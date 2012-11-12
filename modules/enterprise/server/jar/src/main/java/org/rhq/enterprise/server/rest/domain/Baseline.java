package org.rhq.enterprise.server.rest.domain;

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * A baseline
 * @author Heiko W. Rupp
 */
@ApiClass("Representation of a metic baseline/-band")
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

    @ApiProperty("The lower value of the base band")
    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    @ApiProperty("The higher value of the base band")
    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    @ApiProperty("The baseline value (i.e. the average of the metrics")
    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    @ApiProperty("Time this value was computed")
    public long getComputeTime() {
        return computeTime;
    }

    public void setComputeTime(long computeTime) {
        this.computeTime = computeTime;
    }
}
