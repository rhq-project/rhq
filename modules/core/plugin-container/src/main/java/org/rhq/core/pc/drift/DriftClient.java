package org.rhq.core.pc.drift;

import java.io.File;

import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfiguration;

/**
 * This is a client interface to the server that drift-related tasks call to interact with
 * the server.
 */
public interface DriftClient {

    void sendChangeSetToServer(int resourceId, DriftConfiguration driftConfiguration, DriftChangeSetCategory type);

    /**
     * Sends requested content to the server. All of the files in the content directory are
     * zipped up, and the zip file is sent to the server. After the zip file is sent to the
     * server, the content directory is purged.
     *
     * @param resourceId
     * @param driftConfigurationName
     * @param contentDir
     */
    void sendChangeSetContentToServer(int resourceId, String driftConfigurationName, File contentDir);

    File getAbsoluteBaseDirectory(int resourceId, DriftConfiguration driftConfiguration);

}
