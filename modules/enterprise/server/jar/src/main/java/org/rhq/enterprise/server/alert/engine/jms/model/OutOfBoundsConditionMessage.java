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
package org.rhq.enterprise.server.alert.engine.jms.model;

import java.io.Serializable;

/**
 * @author Joseph Marques
 */

public class OutOfBoundsConditionMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int scheduleId;
    private final long timestamp;
    private final double oobValue;

    public OutOfBoundsConditionMessage(int scheduleId, double oobValue, long timestamp) {
        this.scheduleId = scheduleId;
        this.timestamp = timestamp;
        this.oobValue = oobValue;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getOobValue() {
        return oobValue;
    }
}