package org.rhq.enterprise.server.storage.maintenance;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;

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

    private MaintenanceStep baseStep;

    private List<MaintenanceStep> steps = new ArrayList<MaintenanceStep>();

    /**
     * Creates a new job having no steps.
     *
     * @param jobType
     * @param name
     * @param params
     */
    public StorageMaintenanceJob(MaintenanceStep.JobType jobType, String name, Configuration params) {
        this(jobType, name, name, params);
    }

    /**
     * Creates a new job having no steps
     *
     * @param jobType
     * @param name
     * @param description
     * @param params
     */
    public StorageMaintenanceJob(MaintenanceStep.JobType jobType, String name, String description,
        Configuration params) {
        baseStep = new MaintenanceStep()
            .setJobType(jobType)
            .setName(name)
            .setDescription(description)
            .setConfiguration(params.deepCopyWithoutProxies());
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

    /**
     * This constructor should only be used for loading the steps of an existing job from the database. It is assumed
     * that the job and step numbers are already set correctly on each step; therefore, this should <strong>not</strong>
     * be used to create new jobs.
     *
     * @param baseStep
     * @param jobSteps
     */
    public StorageMaintenanceJob(MaintenanceStep baseStep, List<MaintenanceStep> jobSteps) {
        this.baseStep = baseStep;
        steps = jobSteps;
    }

    /**
     * This constructor should only be used for loading the steps of an existing job from the database. It is assumed
     * that the job and step numbers are already set correctly on each step; therefore, this should <strong>not</strong>
     * be used to create new jobs.
     *
     * @param steps
     */
    public StorageMaintenanceJob(List<MaintenanceStep> steps) {
        Preconditions.checkArgument(steps.size() > 0, "Cannot create a maintenance job from an empty list of steps");
        Preconditions.checkArgument(steps.get(0).getStepNumber() == 0, steps.get(0) + " should be a base step");

        baseStep = steps.get(0);
        for (int i = 1; i < steps.size(); ++i) {
            this.steps.add(steps.get(i));
        }
    }

    public MaintenanceStep getBaseStep() {
        return baseStep;
    }

    public void clearSteps() {
        steps.clear();
    }

    public List<MaintenanceStep> getSteps() {
        return steps;
    }

    @Override
    public Iterator<MaintenanceStep> iterator() {
        return steps.iterator();
    }

    public String getJobName() {
        return baseStep.getName();
    }

    public int getJobNumber() {
        return baseStep.getJobNumber();
    }

    public MaintenanceStep.JobType getJobType() {
        return baseStep.getJobType();
    }

    public PropertyMap getJobParameters() {
        return baseStep.getConfiguration().getMap(JobProperties.PARAMETERS);
    }

    public StorageMaintenanceJob addStep(MaintenanceStep step) {
        steps.add(step.setJobNumber(baseStep.getJobNumber()).setJobType(baseStep.getJobType())
            .setStepNumber(steps.size() + 1));
        return this;
    }

    /**
     * Returns a set of all known node addresses from the time the snapshot was created. Note that we will change
     * the return type with a more strongly typed object once the additional fields are added to the StorageNode
     * class for supporting address changes.
     */
    public Set<String> getClusterSnapshot() {
        Configuration configuration = baseStep.getConfiguration();
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

    /**
     * @return The value of the <code>target</code> property which is part of the job-level configuration. This is
     * currently a string which is a node's address. Once storage node data model changes are made, this will likely
     * be a complex type that includes the client (i.e., cql) address, gossip address, the host id, and probably the
     * storage node id.
     */
    public String getTarget() {
        Configuration configuration = baseStep.getConfiguration();
        return configuration.getSimpleValue("target");
    }

    public Configuration getConfiguration() {
        return baseStep.getConfiguration();
    }

    public Property getClusterSnapshotProperty() {
        return baseStep.getConfiguration().get("clusterSnapshot");
    }

    public void setClusterSnapshot(Set<String> clusterSnapshot) {
        Configuration configuration = baseStep.getConfiguration();

        if (configuration == null) {
            configuration = new Configuration();
        }
        PropertyList snapshot = new PropertyList("clusterSnapshot");
        for (String address : clusterSnapshot) {
            // Eventually we will also store the node's host id and listen address in the
            // StorageNode entity. We will want that info in the snapshot.
            snapshot.add(new PropertySimple("address", address));
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
