/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.util.updater;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Same as a {@link FileHashcodeMap} but also holds additional data about
 * changes that were detected after a {@link FileHashcodeMap#rescan}.
 * 
 * @author John Mazzitelli
 */
public class ChangesFileHashcodeMap extends FileHashcodeMap {
    private static final long serialVersionUID = 1L;

    private final Map<String, String> deletions = new HashMap<String, String>();
    private final Map<String, String> additions = new HashMap<String, String>();
    private final Map<String, String> changes = new HashMap<String, String>();
    private final Set<String> ignored = new HashSet<String>();
    private final Set<String> skipped = new HashSet<String>();

    /**
     * Creates an file/hashcode map populated with a map of original file data.
     * Changes/additions/deletions to this original data can be tracked separately
     * in this new object.
     * 
     * This object will copy this original data such that its starting content
     * will mimic the original. As things are added, deleted, changed, this
     * object's internal map should be changed to reflect the new data, as well as
     * that new data individually tracked in the separate
     * {@link #getAdditions()}, {@link #getDeletions()}, {@link #getChanges()} maps. 
     * This is the responsibility of the originator of this map object.
     *
     * @param original contains original file data that this object will copy
     */
    public ChangesFileHashcodeMap(FileHashcodeMap original) {
        putAll(original);
    }

    /**
     * @return the data on files that were deleted from the original
     */
    public Map<String, String> getDeletions() {
        return deletions;
    }

    /**
     * @return the data on files that were added to the original
     */
    public Map<String, String> getAdditions() {
        return additions;
    }

    /**
     * @return the data on files that were changed from the original
     */
    public Map<String, String> getChanges() {
        return changes;
    }

    /**
     * @return the files and directories that were ignored and thus not known if these are true additions or changes
     */
    public Set<String> getIgnored() {
        return ignored;
    }

    /**
     * @return the files and directories located directly under the root deploy dir that were skipped.
     *         These are files/directories that are considered unrelated to the deployment and should be left alone.
     */
    public Set<String> getSkipped() {
        return skipped;
    }
}
