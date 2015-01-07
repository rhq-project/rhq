/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.server.metrics;

/**
 * @author John Sanda
 */
public class StorageClientConstants {

    public static final String REQUEST_WARMUP_PERIOD = "rhq.storage.request.limit.warmup-period";

    public static final String REQUEST_WARMUP_PERIOD_MAX_COUNTER = "rhq.storage.request.limit.max-warmup-counter";

    public static final String REQUEST_TIMEOUT_DAMPENING = "rhq.storage.request.timeout-dampening";

    public static final String REQUEST_TOPOLOGY_CHANGE_DELTA = "rhq.storage.request.limit.topology-delta";

    public static final String LOAD_BALANCING = "rhq.storage.client.load-balancing";

    public static final String DATA_CENTER = "rhq.storage.dc";

    private StorageClientConstants() {
    }

}
