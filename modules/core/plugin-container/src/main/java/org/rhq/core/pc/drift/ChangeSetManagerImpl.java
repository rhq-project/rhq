/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.core.pc.drift;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.ChangeSetWriterImpl;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.util.stream.StreamUtil;

import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;

public class ChangeSetManagerImpl implements ChangeSetManager {

    private File changeSetsDir;

    public ChangeSetManagerImpl(File changeSetsDir) {
        this.changeSetsDir = changeSetsDir;
    }

    @Override
    public boolean changeSetExists(int resourceId, Headers headers) {
        File changeSetDir = findChangeSetDir(resourceId, headers.getDriftConfigurationName());
        if (changeSetDir == null || !changeSetDir.exists()) {
            return false;
        }
        return new File(changeSetDir, "changeset.txt").exists();
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
    public File findChangeSet(int resourceId, String name, DriftChangeSetCategory type) {
        File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
        File changeSetDir = new File(resourceDir, name);
        if (!changeSetDir.exists()) {
            changeSetDir.mkdirs();
        }

        switch (type) {
        case COVERAGE:
            return new File(changeSetDir, "changeset.txt");
        case DRIFT:
            return new File(changeSetDir, "drift-changeset.txt");
        default:
            throw new IllegalArgumentException(type + " is not a recognized, supported change set type.");
        }
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

        File changeSet;
        if (headers.getType() == COVERAGE) {
            changeSet = new File(changeSetDir, "changeset.txt");
        } else {
            changeSet = new File(changeSetDir, "drift-changeset.txt");
        }
        return new ChangeSetWriterImpl(changeSet, headers);
    }

    @Override
    public ChangeSetWriter getChangeSetWriterForUpdate(int resourceId, Headers headers) throws IOException {
        File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
        File changeSetDir = new File(resourceDir, headers.getDriftConfigurationName());

        return new ChangeSetWriterImpl(new File(changeSetDir, "changeset.working"), headers);
    }

    @Override
    public void updateChangeSet(int resourceId, Headers headers) throws IOException {
        File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
        File changeSetDir = new File(resourceDir, headers.getDriftConfigurationName());
        File newChangeSet = new File(changeSetDir, "changeset.working");
        File changeSet = new File(changeSetDir, "changeset.txt");

        if (!newChangeSet.exists()) {
            return;
        }

        changeSet.delete();

        StreamUtil.copy(new BufferedInputStream(new FileInputStream(newChangeSet)),
            new BufferedOutputStream(new FileOutputStream(changeSet)));
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

}
