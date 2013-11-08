/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package org.rhq.enterprise.server.alert.engine.model;

/**
 * Describes the activity state of an alert condition cache element. Because the cache is stateless, we cannot have
 * just "active" and "inactive", because when the cache is reloaded, we really don't know the prior activity state.
 * <p/>
 * Therefore we introduce a 3rd state, unknown, that for all purposes behaves exactly like active but we keep it anyway
 * for clarity reasons.
 * <p/>
 * When a cache is reloaded, the activity of all its elements is initially unknown and then flips from active to
 * inactive based on condition evaluations.
 *
 * @author Lukas Krejci
 * @since 4.10.0
 */
public enum CacheElementActivity {

    UNKNOWN, ACTIVE, INACTIVE;

    public boolean maybeActive() {
        return this != INACTIVE;
    }
}
