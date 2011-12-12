package org.rhq.core.pc.drift;

import org.rhq.core.domain.drift.DriftDefinition;

import java.io.File;

class DriftClientTestStub implements DriftClient {

    private File basedir;

    private boolean failingOnSendChangeSet;

    private int sendChangeSetInvocationCount;

    private int reportMissingBaseDirInvocationCount;

    private int sendChangeSetContentInvocationCount;

    @Override
    public void sendChangeSetToServer(DriftDetectionSummary detectionSummary) {
        ++sendChangeSetInvocationCount;
        if (failingOnSendChangeSet) {
            throw new RuntimeException("Failed to send change set to server");
        }
    }

    public int getSendChangeSetInvocationCount() {
        return sendChangeSetInvocationCount;
    }

    @Override
    public void sendChangeSetContentToServer(int resourceId, String driftDefinitionName, File contentDir) {
        ++sendChangeSetContentInvocationCount;
    }

    public int getSendChangeSetContentInvocationCount() {
        return sendChangeSetContentInvocationCount;
    }

    @Override
    public void repeatChangeSet(int resourceId, String driftDefName, int version) {
    }

    @Override
    public File getAbsoluteBaseDirectory(int resourceId, DriftDefinition driftDefinition) {
        return basedir;
    }

    @Override
    public void reportMissingBaseDir(int resourceId, DriftDefinition driftDefinition) {
        ++reportMissingBaseDirInvocationCount;
    }

    public int getReportMissingBaseDirInvocationCount() {
        return reportMissingBaseDirInvocationCount;
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
