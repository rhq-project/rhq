package org.rhq.core.pc.drift;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.ChangeSetWriterImpl;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfiguration;

import static java.io.File.separator;

public class ChangeSetManagerImpl implements ChangeSetManager {

    private File changeSetsDir;

    public ChangeSetManagerImpl(File changeSetsDir) {
        this.changeSetsDir = changeSetsDir;
    }

    @Override
    public File findChangeSet(int resourceId, String driftConfigurationName) throws IOException {
        File changeSetDir = findChangeSetDir(resourceId, driftConfigurationName);
        File changeSetFile = new File(changeSetDir, "changeset.txt");

        if (changeSetFile.exists()) {
            return changeSetFile;
        }

        return null;
    }

    @Override
    public ChangeSetReader getChangeSetReader(int resourceId, String driftConfigurationName)
        throws IOException {
        File changeSetFile = findChangeSet(resourceId, driftConfigurationName);

        if (changeSetFile == null) {
            return null;
        }

        return new ChangeSetReaderImpl(changeSetFile);
    }

    @Override
    public ChangeSetWriter getChangeSetWriter(int resourceId, Headers headers) throws IOException {
        File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
        File changeSetDir = new File(resourceDir, headers.getDriftConfigurationName());

        if (!changeSetDir.exists()) {
            changeSetDir.mkdirs();
        }

        return new ChangeSetWriterImpl(new File(changeSetDir, "changeset.txt"), headers);
    }

    @Override
    public void addFileToChangeSet(int resourceId, DriftConfiguration driftConfiguration, File file) {
    }

    private File findChangeSetDir(int resourceId, String driftConfigurationName) {
        File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
        if (!resourceDir.exists()) {
            return null;
        }

        return new File(resourceDir, driftConfigurationName);

    }

    private String relativePath(File basedir, File file) {
        if (basedir.equals(file)) {
            return basedir.getPath();
        }
        return FilenameUtils.getName(basedir.getAbsolutePath()) + separator +
            file.getAbsolutePath().substring(basedir.getAbsolutePath().length() + 1);
    }
}
