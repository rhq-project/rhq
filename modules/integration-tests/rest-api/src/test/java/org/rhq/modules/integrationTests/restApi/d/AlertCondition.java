package org.rhq.modules.integrationTests.restApi.d;

/**
 * Alert condition for testing the rest interface
 * @author Heiko W. Rupp
 */
public class AlertCondition {

    int id;
    String name; // = AlertConditionOperator.class
    String category;
    private Double threshold;
    private String option;
    private Integer triggerId;
    private String comparator;
    private int measurementDefinition;

    public AlertCondition(String name, String category) {
        this.name = name;
        this.category = category;
    }

    public AlertCondition() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * The name of the condition. This is one of AlertConditionOperator
     * @return the name of the condition
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public Integer getTriggerId() {
        return triggerId;
    }

    public void setTriggerId(Integer triggerId) {
        this.triggerId = triggerId;
    }

    public String getComparator() {
        return comparator;
    }

    public void setComparator(String comparator) {
        this.comparator = comparator;
    }

    public void setMeasurementDefinition(int measurementDefinition) {
        this.measurementDefinition = measurementDefinition;
    }

    public int getMeasurementDefinition() {
        return measurementDefinition;
    }
}
