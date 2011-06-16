package org.rhq.core.pc.drift;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.core.domain.drift.DriftConfiguration;

import static java.io.File.separator;

public class ChangeSetManagerImpl implements ChangeSetManager {

    private File changeSetsDir;

    public ChangeSetManagerImpl(File changeSetsDir) {
        this.changeSetsDir = changeSetsDir;
    }

    @Override
    public ChangeSetReader getChangeSetReader(int resourceId, DriftConfiguration driftConfiguration)
        throws IOException {
        File changeSetDir = findChangeSetDir(resourceId, driftConfiguration);
        File changeSetFile = new File(changeSetDir, "changeset.txt");

        if (!changeSetFile.exists()) {
            return null;
        }

        return new ChangeSetReaderImpl(changeSetFile);
    }

    @Override
    public ChangeSetWriter getChangeSetWriter(int resourceId, DriftConfiguration driftConfiguration) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addFileToChangeSet(int resourceId, DriftConfiguration driftConfiguration, File file) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private File findChangeSetDir(int resourceId, DriftConfiguration driftConfiguration) {
        File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
        if (!resourceDir.exists()) {
            return null;
        }

        return new File(resourceDir, driftConfiguration.getName());

    }

    private String relativePath(File basedir, File file) {
        if (basedir.equals(file)) {
            return basedir.getPath();
        }
        return FilenameUtils.getName(basedir.getAbsolutePath()) + separator +
            file.getAbsolutePath().substring(basedir.getAbsolutePath().length() + 1);
    }
}
