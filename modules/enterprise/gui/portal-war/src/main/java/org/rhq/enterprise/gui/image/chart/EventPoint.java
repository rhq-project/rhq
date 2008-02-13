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
package org.rhq.enterprise.gui.image.chart;

import org.rhq.enterprise.gui.image.data.IEventPoint;

public class EventPoint implements IEventPoint {
    private int m_id;
    private long m_timestamp;

    public EventPoint(int id, long timestamp) {
        this.m_id = id;
        this.m_timestamp = timestamp;
    }

    /**
     * @return Returns the event identifier.
     */
    public int getEventID() {
        return this.m_id;
    }

    /**
     * @return The absolute time of the datum, in milliseconds.
     */
    public long getTimestamp() {
        return this.m_timestamp;
    }
}