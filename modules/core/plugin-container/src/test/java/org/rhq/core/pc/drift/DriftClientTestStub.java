package org.rhq.core.pc.drift;

import java.io.File;

import org.rhq.core.domain.drift.DriftDefinition;

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
    public void sendChangeSetContentToServer(int resourceId, String driftDefinitionName, File contentDir) {
    }

    @Override
    public void repeatChangeSet(int resourceId, String driftDefName, int version) {
    }

    @Override
    public File getAbsoluteBaseDirectory(int resourceId, DriftDefinition driftDefinition) {
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
