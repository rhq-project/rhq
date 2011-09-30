package org.rhq.core.pc.drift;

import java.io.File;

import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftDefinition;

class DriftClientTestStub implements DriftClient {

    private File basedir;

    @Override
    public void sendChangeSetToServer(int resourceId, DriftDefinition driftConfiguration,
        DriftChangeSetCategory type) {

    }

    @Override
    public void sendChangeSetContentToServer(int resourceId, String driftConfigurationName, File contentDir) {
    }

    @Override
    public File getAbsoluteBaseDirectory(int resourceId, DriftDefinition driftConfiguration) {
        return basedir;
    }

    public void setBaseDir(File basedir) {
        this.basedir = basedir;
    }
}
