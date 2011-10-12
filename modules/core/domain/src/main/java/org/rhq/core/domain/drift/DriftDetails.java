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

package org.rhq.core.domain.drift;

import java.io.Serializable;

/**
 * Encapsulates information and details about drift that can be used in the UI. This class
 * the Drift object, the change set to which the drift belongs, and the change set in which
 * the file with drift was last referenced. This class also store statuses of the current
 * and previous versions of the file to indicate whether or not the content is in the
 * database. Lastly, it has a flag to indicate whether or not the file is a binary file.
 */
public class DriftDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    private Drift<?, ?> drift;

    private DriftChangeSet<?> previousChangeSet;

    private DriftFileStatus newFileStatus;

    private DriftFileStatus oldFileStatus;

    private boolean isBinary;

    /** @return The drift object */
    public Drift<?, ?> getDrift() {
        return drift;
    }

    /** @param drift The Drift object to which the details belong */
    public void setDrift(Drift<?, ?> drift) {
        this.drift = drift;
    }

    /** @return The change set to which the drift is part of */
    public DriftChangeSet<?> getChangeSet() {
        return drift.getChangeSet();
    }

    /**
     * Returns the previous change set for which there was an occurrence of drift on the
     * file referenced by the Drift object. This can be null if there was no previous
     * drift.
     *
     * @return The previous change set for which there was drift on the file referenced by
     * the Drift object or null if there is not previous drift on the file.
     */
    public DriftChangeSet<?> getPreviousChangeSet() {
        return previousChangeSet;
    }

    /**
     * @param changeSet The previous change set in which drift occurred on the file
     * referenced by the Drift object.
     */
    public void setPreviousChangeSet(DriftChangeSet<?> changeSet) {
        previousChangeSet = changeSet;
    }

    /**
     * @return The status of the file as reported in the current change set. Indicates
     * whether or not the file has been loaded into the database. Can be null if the type
     * of drift is a {@link DriftCategory#FILE_REMOVED deletion}.
     */
    public DriftFileStatus getNewFileStatus() {
        return newFileStatus;
    }

    /**
     * @param status The status of the file reported in the change set.
     */
    public void setNewFileStatus(DriftFileStatus status) {
        newFileStatus = status;
    }

    /**
     * @return The status of the file as reported in the previous change set. Indicates
     * whether or not the file has been loaded into the database. Can be null if the type
     * of drift is a {@link DriftCategory#FILE_ADDED addition}.
     */
    public DriftFileStatus getOldFileStatus() {
        return oldFileStatus;
    }

    /**
     * @param status The status of the file as reported in the last change set in which the
     * file was referenced.
     */
    public void setOldFileStatus(DriftFileStatus status) {
        oldFileStatus = status;
    }

    /**
     * @return true if the file is a binary file.
     */
    public boolean isBinaryFile() {
        return isBinary;
    }

    /**
     * @param binaryFile True if the file is a binary type, false otherwise.
     */
    public void setBinaryFile(boolean binaryFile) {
        isBinary = binaryFile;
    }
}
