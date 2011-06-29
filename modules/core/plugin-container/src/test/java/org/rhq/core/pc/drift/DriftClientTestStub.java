package org.rhq.core.pc.drift;

import java.io.File;

import org.rhq.core.domain.drift.DriftConfiguration;

class DriftClientTestStub implements DriftClient {

    private File basedir;

    @Override
    public void sendChangeSetToServer(int resourceId, DriftConfiguration driftConfiguration) {
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
}
