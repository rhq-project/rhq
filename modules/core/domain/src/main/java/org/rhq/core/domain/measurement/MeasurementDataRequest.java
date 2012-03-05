package org.rhq.core.domain.measurement;

import java.io.Serializable;

public class MeasurementDataRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int scheduleId;

    /** The meausurement name */
    private String name;

    /** The meausurement's data type */
    private DataType type;

    private NumericType rawNumericType;

    /**
     * Creates a new request for live measurement data.
     * @param scheduleId The id of the metric schedule
     * @param name The measurement name
     * @param type The measurement's type
     * @param rawNumericType The measurement's raw data type
     */
    public MeasurementDataRequest(int scheduleId, String name, DataType type, NumericType rawNumericType) {
        this.scheduleId = scheduleId;
        this.name = name;
        this.type = type;
        this.rawNumericType = rawNumericType;
    }

    public MeasurementDataRequest(String metricName, DataType type) {
        name = metricName;
        this.type = type;
    }

    /**
     * @return The requested measurement schedule id
     */
    public int getScheduleId() {
        return scheduleId;
    }

    /**
     * @return The requested measurement's name
     */
    public String getName() {
        return name;
    }

    /**
     * @return The requested measurement's data type
     */
    public DataType getType() {
        return type;
    }

    /**
     * @return The requested measurement's raw numeric type
     */
    public NumericType getRawNumericType() {
        return rawNumericType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (obj instanceof MeasurementDataRequest) {
            MeasurementDataRequest that = (MeasurementDataRequest) obj;
            return this.name.equals(that.name) && this.type.equals(that.name);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 41 * (name.hashCode() + type.hashCode());
    }

}
