package org.rhq.enterprise.server.rest.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * One numerical data point of a schedule.
 * This point does not contain the schedule id, as it is expected
 * to be used to return lists of data points for a schedule.
 *
 * Xml names for the item and the attributes are shortened to
 * conserve space when transferring to clients
 *
 * @author Heiko W. Rupp
 */
@ApiClass("One numerical data point of a metric (aka schedule)")
@XmlRootElement
public class NumericDataPoint {

    long timeStamp;
    Double value;
    private int scheduleId;

    @SuppressWarnings("unused")
    public NumericDataPoint() {
        // Needed for JAXB
    }

    public NumericDataPoint(long timeStamp, Double value) {
        this.timeStamp = timeStamp;
        this.value = value;
    }

    public NumericDataPoint(long timeStamp, int scheduleId, Double value) {
        this.timeStamp = timeStamp;
        this.scheduleId = scheduleId;
        this.value = value;
    }

    @ApiProperty("The timestamp when the metric was taken")
    @XmlAttribute
    public long getTimeStamp() {
        return timeStamp;
    }

    @ApiProperty("The numerical value")
    @XmlAttribute
    public Double getValue() {
        return value;
    }

    @ApiProperty("The id of the metric")
    @XmlAttribute
    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
