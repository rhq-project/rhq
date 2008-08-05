package org.rhq.core.domain.resource.composite;

import org.rhq.core.domain.resource.ResourceType;

import java.io.Serializable;

// implement Comparable because these are to be put in a sorted set
public class ResourceTypeTemplateCountComposite implements Serializable, Comparable<ResourceTypeTemplateCountComposite> {

    private static final long serialVersionUID = 1L;

    private final ResourceType type;
    private final long enabledMetricCount;
    private final long disabledMetricCount;
    private final long enabledAlertCount;
    private final long disabledAlertCount;

    public ResourceTypeTemplateCountComposite(ResourceType type, long enabledMetricCount, long disabledMetricCount,
        long enabledAlertCount, long disabledAlertCount) {
        super();
        this.type = type;
        this.enabledMetricCount = enabledMetricCount;
        this.disabledMetricCount = disabledMetricCount;
        this.enabledAlertCount = enabledAlertCount;
        this.disabledAlertCount = disabledAlertCount;
    }

    public ResourceType getType() {
        return type;
    }

    public long getEnabledMetricCount() {
        return enabledMetricCount;
    }

    public long getDisabledMetricCount() {
        return disabledMetricCount;
    }

    public long getEnabledAlertCount() {
        return enabledAlertCount;
    }

    public long getDisabledAlertCount() {
        return disabledAlertCount;
    }

    public int compareTo(ResourceTypeTemplateCountComposite other) {
        return this.type.compareTo(other.type);
    }

    @Override
    public String toString() {
        return "ResourceTypeTemplateCountComposite[ " + "type[" + "id=" + getType().getId() + ", " + "name="
            + getType().getName() + "], " + "metricTemplateCounts(" + enabledMetricCount + " / " + disabledMetricCount
            + "), " + "metricAlertCounts(" + enabledAlertCount + " / " + disabledAlertCount + ") ]";
    }
}
