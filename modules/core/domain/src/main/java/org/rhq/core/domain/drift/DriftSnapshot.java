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
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import static org.rhq.core.domain.drift.DriftCategory.FILE_REMOVED;

/**
 * A representation of an agent's drift file monitoring. 
 * 
 * @author John Sanda
 */
public class DriftSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The version is initially -1 which indicates that there are no
     * change sets as opposed to an empty change set.
     */
    private int version = -1;

    private Map<String, Drift<?, ?>> entries = new TreeMap<String, Drift<?, ?>>();

    public int getVersion() {
        return version;
    }

    public Collection<Drift<?, ?>> getEntries() {
        return entries.values();
    }

    public <D extends Drift<?, ?>> DriftSnapshot add(DriftChangeSet<D> changeSet) {
        for (Drift<?, ?> entry : changeSet.getDrifts()) {
            entries.remove(entry.getPath());
            if (entry.getCategory() != FILE_REMOVED) {
                entries.put(entry.getPath(), entry);
            }
        }
        version = changeSet.getVersion();
        return this;
    }

    public DriftDiffReport<?> diff(DriftSnapshot right) {
        DriftSnapshot left = this;
        DriftDiffReport<Drift<?, ?>> diff = new DriftDiffReport<Drift<?, ?>>();

        for (Map.Entry<String, Drift<?, ?>> entry : left.entries.entrySet()) {
            if (!right.entries.containsKey(entry.getKey())) {
                diff.elementNotInRight(entry.getValue());
            }
        }

        for (Map.Entry<String, Drift<?, ?>> entry : right.entries.entrySet()) {
            if (!left.entries.containsKey(entry.getKey())) {
                diff.elementNotInLeft(entry.getValue());
            }
        }

        for (Map.Entry<String, Drift<?, ?>> entry : left.entries.entrySet()) {
            Drift<?, ?> rightDrift = right.entries.get(entry.getKey());
            if (rightDrift != null) {
                DriftFile leftFile = entry.getValue().getNewDriftFile();
                DriftFile rightFile = rightDrift.getNewDriftFile();

                if (!leftFile.getHashId().equals(rightFile.getHashId())) {
                    diff.elementInConflict(entry.getValue());
                }
            }
        }

        return diff;
    }

}
