package org.rhq.core.pc.drift;

import java.io.File;

import org.rhq.core.domain.drift.DriftConfiguration;

class DriftClientTestStub implements DriftClient {

    private File basedir;

    private boolean failingOnSendChangeSet;

    @Override
    public void sendChangeSetToServer(DriftDetectionSummary detectionSummary) {
        if (failingOnSendChangeSet) {
            throw new RuntimeException("Failed to send change set to server");
        }
    }

    @Override
    public void sendChangeSetContentToServer(int resourceId, String driftConfigurationName, File contentDir) {
    }

    @Override
    public File getAbsoluteBaseDirectory(int resourceId, DriftConfiguration driftConfiguration) {
        return basedir;
    }

    public void setBaseDir(File basedir) {
        this.basedir = basedir;
    }

    public boolean isFailingOnSendChangeSet() {
        return failingOnSendChangeSet;
    }

    public void setFailingOnSendChangeSet(boolean failing) {
        failingOnSendChangeSet = failing;
    }
}
