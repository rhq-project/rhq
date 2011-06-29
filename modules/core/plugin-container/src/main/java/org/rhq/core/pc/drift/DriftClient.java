package org.rhq.core.pc.drift;

import java.io.File;

import org.rhq.core.domain.drift.DriftConfiguration;

/**
 * This is a client interface to the server that drift-related tasks call to interact with
 * the server.
 */
public interface DriftClient {

    void sendChangeSetToServer(int resourceId, DriftConfiguration driftConfiguration);

    void sendChangeSetContentToServer(int resourceId, String driftConfigurationName, File contentDir);

    File getAbsoluteBaseDirectory(DriftConfiguration driftConfiguration);

}
