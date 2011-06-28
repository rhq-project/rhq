package org.rhq.core.pc.drift;

import java.io.File;
import java.io.IOException;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfiguration;

public interface ChangeSetManager {

    /**
     * Locates the latest change set for the specified resource id and drift configuration.
     *
     * @param resourceId The id of the resource to which the change set belongs
     * @param driftConfiguration The drift configuration for which the change set was generated
     * @return The change set file or null if it is not found
     * @throws IOException
     */
    File findChangeSet(int resourceId, DriftConfiguration driftConfiguration) throws IOException;

    /**
     * Locates the latest change set for the given resource and drift configuration and
     * returns a ChangeSetReader for that change set. Note that a resource can have
     * multiple drift configurations; so, both the resource id and the drift configuration
     * are required to uniquely identify a particular change set.
     *
     * @param resourceId The id of the resource to which the change set belongs
     * @param driftConfiguration The drift configuration for which the change set was generated
     * @return A ChangeSetReader that is open on the change set identified by resourceId
     * and driftConfiguration. Returns null if no change set has previously been generated.
     * @see ChangeSetReader
     */
    ChangeSetReader getChangeSetReader(int resourceId, DriftConfiguration driftConfiguration) throws IOException;

    ChangeSetWriter getChangeSetWriter(int resourceId, DriftConfiguration driftConfiguration,
        DriftChangeSetCategory type) throws IOException;

    void addFileToChangeSet(int resourceId, DriftConfiguration driftConfiguration, File file);

}
