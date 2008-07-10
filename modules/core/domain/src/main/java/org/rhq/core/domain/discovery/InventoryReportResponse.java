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
package org.rhq.core.domain.discovery;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This report is sent in response to an InventoryReport being sent to the server. It tells the agent what the persistent
 * resource ids are. TODO GH: This could also be used for a periodic sync when things get out of whack. (The collection,
 * transmission and persistence of inventory is not transactional between the server and agent)
 *
 * @author Greg Hinkle
 */
@Deprecated
public class InventoryReportResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Maps the agent declared uuid to the resource's true "id"
     */
    private Map<String, Integer> uuidToIntegerMapping = new LinkedHashMap<String, Integer>();

    public Map<String, Integer> getUuidToIntegerMapping() {
        return uuidToIntegerMapping;
    }

    public void setUuidToIntegerMapping(Map<String, Integer> uuidToIntegerMapping) {
        this.uuidToIntegerMapping = uuidToIntegerMapping;
    }

    public void addIdMapping(String uuid, Integer persistentId) {
        this.uuidToIntegerMapping.put(uuid, persistentId);
    }


}