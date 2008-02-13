/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.alert.engine;

public class AlertConditionCacheStats {
    public int created;
    public int updated;
    public int deleted;
    public int matched;

    public void add(AlertConditionCacheStats stats) {
        this.created += stats.created;
        this.updated += stats.updated;
        this.deleted += stats.deleted;
        this.matched += stats.matched;
    }

    @Override
    public String toString() {
        return (getClass().getSimpleName() + "[ " + "created=" + created + ", " + "updated=" + updated + ", "
            + "deleted=" + deleted + ", " + "matched=" + matched + " ]");
    }
}