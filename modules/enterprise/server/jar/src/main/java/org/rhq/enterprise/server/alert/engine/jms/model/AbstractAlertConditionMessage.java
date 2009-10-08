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

public abstract class AbstractAlertConditionMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int alertConditionId;
    private final long timestamp;

    protected AbstractAlertConditionMessage(int alertConditionId, long timestamp) {
        this.alertConditionId = alertConditionId;
        this.timestamp = timestamp;
    }

    public int getAlertConditionId() {
        return alertConditionId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "AbstractAlertConditionMessage" + "[ " + "alertConditionId=" + alertConditionId + ", " + "timestamp="
            + timestamp + " ]";
    }
}