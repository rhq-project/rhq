/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.core.domain.drift;

/**
 * Type of change set report. There are two types of change sets, coverage and drift. A
 * coverage change set is in effect a snapshot of the files that are being monitored for
 * drift detection. A drift change set on the hand, represents a delta between one or more
 * files on disk and their versions in the previous change set.
 * <p/>
 * A DriftChangeSetCategory has a single representation which is suitable for persisting
 * in a database.
 *
 * @author Jay Shaughnesssy
 */
public enum DriftChangeSetCategory {
    /**
     * A coverage change set is a snapshot of the files that are being monitored for drift
     * detection.
     */
    COVERAGE("C"), // Reports only on files being covered by a drift definition.

    /**
     * A drift change set represents a change between one or more files and their versions
     * in the previous change set.
     */
    DRIFT("D"); // Reports on actual drift.

    private final String code;

    DriftChangeSetCategory(String code) {
        this.code = code;
    }

    /** @return The single character code */
    public String code() {
        return code;
    }

    /**
     * Parses the single character code into a DriftChangeSetCategory object.
     *
     * @param code A single character code
     * @return The corresponding DriftChangeSetCategory
     * @throws IllegalArgumentException if the code is not recognized
     */
    public static DriftChangeSetCategory fromCode(String code) {
        for (DriftChangeSetCategory type : DriftChangeSetCategory.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException(code + " is not a DriftChangeSetCategory code");
    }

}