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

import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;

import java.io.File;
import java.io.IOException;

import org.rhq.common.drift.ChangeSetReader;
import org.rhq.common.drift.ChangeSetReaderImpl;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.ChangeSetWriterImpl;
import org.rhq.common.drift.Headers;
import org.rhq.core.domain.drift.DriftChangeSetCategory;

public class ChangeSetManagerImpl implements ChangeSetManager {

    private File changeSetsDir;

    public ChangeSetManagerImpl(File changeSetsDir) {
        this.changeSetsDir = changeSetsDir;
    }

    @Override
    public boolean changeSetExists(int resourceId, Headers headers) {
        File changeSetDir = findChangeSetDir(resourceId, headers.getDriftDefinitionName());
        if (changeSetDir == null || !changeSetDir.exists()) {
            return false;
        }
        return new File(changeSetDir, DriftDetector.FILE_CHANGESET_FULL).exists();
    }

    @Override
    public File findChangeSet(int resourceId, String driftDefinitionName) throws IOException {
        File changeSetDir = findChangeSetDir(resourceId, driftDefinitionName);
        File changeSetFile = new File(changeSetDir, DriftDetector.FILE_CHANGESET_FULL);

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
            return new File(changeSetDir, DriftDetector.FILE_CHANGESET_FULL);
        case DRIFT:
            return new File(changeSetDir, DriftDetector.FILE_CHANGESET_DELTA);
        default:
            throw new IllegalArgumentException(type + " is not a recognized, supported change set type.");
        }
    }

    @Override
    public ChangeSetReader getChangeSetReader(int resourceId, String driftDefinitionName) throws IOException {
        File changeSetFile = findChangeSet(resourceId, driftDefinitionName);

        if (changeSetFile == null) {
            return null;
        }

        return new ChangeSetReaderImpl(changeSetFile);
    }

    @Override
    public ChangeSetReader getChangeSetReader(File changeSetFile) throws IOException {
        return new ChangeSetReaderImpl(changeSetFile);
    }

    @Override
    public ChangeSetWriter getChangeSetWriter(int resourceId, Headers headers) throws IOException {
        File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
        File changeSetDir = new File(resourceDir, headers.getDriftDefinitionName());

        if (!changeSetDir.exists()) {
            changeSetDir.mkdirs();
        }

        File changeSet;
        if (headers.getType() == COVERAGE) {
            changeSet = new File(changeSetDir, DriftDetector.FILE_CHANGESET_FULL);
        } else {
            changeSet = new File(changeSetDir, DriftDetector.FILE_CHANGESET_DELTA);
        }
        return new ChangeSetWriterImpl(changeSet, headers);
    }

    @Override
    public ChangeSetWriter getChangeSetWriter(File changeSetFile, Headers headers) throws IOException {
        return new ChangeSetWriterImpl(changeSetFile, headers);
    }

    private File findChangeSetDir(int resourceId, String driftDefinitionName) {
        File resourceDir = new File(changeSetsDir, Integer.toString(resourceId));
        if (!resourceDir.exists()) {
            return null;
        }

        return new File(resourceDir, driftDefinitionName);

    }

}
