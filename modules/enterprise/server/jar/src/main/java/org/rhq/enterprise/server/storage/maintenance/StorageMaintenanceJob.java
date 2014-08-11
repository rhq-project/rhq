package org.rhq.enterprise.server.storage.maintenance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.storage.MaintenanceStep;

/**
 * @author John Sanda
 */
public class StorageMaintenanceJob implements Serializable, Iterable<MaintenanceStep>  {

    public static final long serialVersionUID = 1L;

    private List<MaintenanceStep> steps = new ArrayList<MaintenanceStep>();

    public StorageMaintenanceJob(MaintenanceStep.JobType jobType, String name, Configuration params) {
        this(jobType, name, name, params);
    }

    public StorageMaintenanceJob(MaintenanceStep.JobType jobType, String name, String description,
        Configuration params) {
        steps.add(new MaintenanceStep()
            .setJobType(jobType)
            .setName(name)
            .setDescription(description)
            .setConfiguration(convert(params)));
    }

    public StorageMaintenanceJob(MaintenanceStep baseStep) {
        Preconditions.checkArgument(baseStep.getStepNumber() == 0, baseStep + " should be a base step");
        steps.add(baseStep);
    }

    private Configuration convert(Configuration params) {
        Configuration configuration = new Configuration();
        PropertyMap propertyMap = new PropertyMap("parameters");
        for (Property p : params.getProperties()) {
            propertyMap.put(p.deepCopy(false));
        }
        configuration.put(propertyMap);

        return configuration;
    }

    public StorageMaintenanceJob(List<MaintenanceStep> steps) {
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Cannot create a maintenance job from an empty list of steps");
        }
        this.steps = steps;
    }

    public MaintenanceStep getBaseStep() {
        return steps.get(0);
    }

    public boolean hasSteps() {
        return steps.size() > 1;
    }

    @Override
    public Iterator<MaintenanceStep> iterator() {
        return steps.subList(1, steps.size()).iterator();
    }

    public int getJobNumber() {
        return getBaseStep().getJobNumber();
    }

    public MaintenanceStep.JobType getJobType() {
        return getBaseStep().getJobType();
    }

    public PropertyMap getJobParameters() {
        return getBaseStep().getConfiguration().getMap("parameters");
    }

    public void addStep(MaintenanceStep step) {
        steps.add(step);
    }

    /**
     * Returns a set of all known node addresses from the time the snapshot was created.
     */
    public Set<String> getClusterSnapshot() {
        Configuration configuration = getBaseStep().getConfiguration();
        PropertyList propertyList = (PropertyList) configuration.get("clusterSnapshot");

        if (propertyList == null) {
            return Collections.emptySet();
        }

        Set<String> snapshot = new HashSet<String>();

        for (Property p : propertyList.getList()) {
            PropertySimple simple = (PropertySimple) p;
            snapshot.add(simple.getStringValue());
        }

        return snapshot;
    }

    public void setClusterSnapshot(List<StorageNode> clusterNodes) {
        MaintenanceStep baseStep = getBaseStep();
        Configuration configuration = baseStep.getConfiguration();

        if (configuration == null) {
            configuration = new Configuration();
        }
        PropertyList snapshot = new PropertyList("clusterSnapshot");
        for (StorageNode node : clusterNodes) {
            // Eventually we will also store the node's host id and listen address in the
            // StorageNode entity. We will want that info in the snapshot.
            snapshot.add(new PropertySimple("address", node.getAddress()));
        }
        configuration.put(snapshot);
        baseStep.setConfiguration(configuration);
    }

    @Override
    public String toString() {
        return "StorageMaintenanceJob[jobName = " + getBaseStep().getName() + ", jobNumber = " +
            getBaseStep().getJobNumber() + ", jobType = " + getBaseStep().getJobType() + "]";
    }
}
