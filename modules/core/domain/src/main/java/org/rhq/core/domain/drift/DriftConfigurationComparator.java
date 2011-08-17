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

import java.util.Comparator;
import java.util.List;

/**
 * Compares two {@link DriftConfiguration} objects. The comparison can either
 * compare basedir/includes-excludes filters or ignore them. Will also compare
 * name, interval, enabled flag.
 * 
 * @author John Mazzitelli
 */
public class DriftConfigurationComparator implements Comparator<DriftConfiguration> {

    public enum CompareMode {
        /**
         * The comparator will only check the base information: name, enabled, interval.
         * The basedir/includes-excludes filters will be ignored. 
         */
        ONLY_BASE_INFO,

        /**
         * The comparator will only check the basedir/includes-excludes filters. 
         * The base information will be ignored (see {@link #ONLY_BASE_INFO} for what those are).
         */
        ONLY_DIRECTORY_SPECIFICATIONS,

        /**
         * The comparator will check both the basedir/includes-excludes filters and 
         * the base information (see {@link #ONLY_BASE_INFO} for what those are).
         */
        BOTH_BASE_INFO_AND_DIRECTORY_SPECIFICATIONS;
    };

    private final CompareMode compareMode;

    public DriftConfigurationComparator(CompareMode mode) {
        this.compareMode = mode;
    }

    @Override
    public int compare(DriftConfiguration dc1, DriftConfiguration dc2) {
        if (compareMode != CompareMode.ONLY_DIRECTORY_SPECIFICATIONS) {
            if (dc1.getName() != null) {
                if (dc2.getName() != null) {
                    int results = dc1.getName().compareTo(dc2.getName());
                    if (results != 0) {
                        return results;
                    }
                } else {
                    return 1; // dc1's name is not null, but dc2's name is null, not equal!
                }
            } else if (dc2.getName() != null) {
                return -1; // dc1's name is null, but dc2's name is not null, not equal!
            }

            if (dc1.getInterval() != dc2.getInterval()) {
                return dc1.getInterval() < dc2.getInterval() ? -1 : 1;
            }

            if (dc1.isEnabled() != dc2.isEnabled()) {
                return dc1.isEnabled() ? 1 : -1; // so false sorts before true, seems logical to me
            }
        }

        if (compareMode != CompareMode.ONLY_BASE_INFO) {
            if (dc1.getBasedir() != null) {
                if (!dc1.getBasedir().equals(dc2.getBasedir())) {
                    int hash1 = dc1.getBasedir().hashCode();
                    int hash2 = (dc2.getBasedir() != null) ? dc2.getBasedir().hashCode() : 0;
                    return hash1 < hash2 ? -1 : 1;
                }
            } else if (dc2.getBasedir() != null) {
                return -1; // dc1's basedir is null, but dc2's basedir is not null, not equal!
            }

            List<Filter> filters1 = dc1.getIncludes();
            List<Filter> filters2 = dc2.getIncludes();
            int results = compareFilters(filters1, filters2);
            if (results != 0) {
                return results;
            }

            filters1 = dc1.getExcludes();
            filters2 = dc2.getExcludes();
            results = compareFilters(filters1, filters2);
            if (results != 0) {
                return results;
            }
        }

        return 0;
    }

    private int compareFilters(List<Filter> filters1, List<Filter> filters2) {
        if (filters1 == null) {
            return (filters2 == null) ? 0 : -1;
        } else if (filters2 == null) {
            return 1;
        }

        // at this point, we know both filters1 and filters2 are non-null
        // let's do the next easiest check - size. If the lists are different sizes, then clearly they are different
        if (filters1.size() != filters2.size()) {
            return (filters1.size() < filters2.size()) ? -1 : 1;
        }

        // they are the same size, let's see if they have the same items. We only need to see if one contains
        // all of the other; because they are the same size, if filters1 contains all the filters2 items,
        // then clearly the other way is true too (filters2 contains all of filters1 items).
        if (!filters1.containsAll(filters2)) {
            return -1; // unsure if we need to do further anaylsis to determine if -1 or 1 should be returned; for now, don't worry about it
        }

        return 0;
    }
}
