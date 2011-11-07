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

import java.io.File;

import org.rhq.core.domain.drift.DriftChangeSetCategory;

public class DriftDetectionSummary {

    private DriftDetectionSchedule schedule;

    private DriftChangeSetCategory type;

    private File oldSnapshot;

    private File newSnapshot;

    private File driftChangeSet;

    private boolean repeat;

    private boolean baseDirExists = true;

    private int version;

    public DriftDetectionSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(DriftDetectionSchedule schedule) {
        this.schedule = schedule;
    }

    public DriftChangeSetCategory getType() {
        return type;
    }

    public void setType(DriftChangeSetCategory type) {
        this.type = type;
    }

    public File getOldSnapshot() {
        return oldSnapshot;
    }

    public void setOldSnapshot(File oldSnapshot) {
        this.oldSnapshot = oldSnapshot;
    }

    public File getNewSnapshot() {
        return newSnapshot;
    }

    public void setNewSnapshot(File newSnapshot) {
        this.newSnapshot = newSnapshot;
    }

    public File getDriftChangeSet() {
        return driftChangeSet;
    }

    public void setDriftChangeSet(File driftChangeSet) {
        this.driftChangeSet = driftChangeSet;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isBaseDirExists() {
        return baseDirExists;
    }

    public void setBaseDirExists(boolean exists) {
        baseDirExists = exists;
    }

}
