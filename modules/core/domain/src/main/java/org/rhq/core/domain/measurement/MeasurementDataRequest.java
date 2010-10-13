package org.rhq.core.domain.measurement;

import java.io.Serializable;

public class MeasurementDataRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The meausurement name */
    private String name;

    /** The meausurement's data type */
    private DataType type;

    public MeasurementDataRequest(MeasurementDefinition definition) {
        name = definition.getName();
        type = definition.getDataType();
    }

    public MeasurementDataRequest(String metricName, DataType type) {
        name = metricName;
        this.type = type;
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
