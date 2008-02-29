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

public class ActiveAlertConditionMessage extends AbstractAlertConditionMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String value;

    public ActiveAlertConditionMessage(int alertConditionId, long timestamp, String value, Object... extraParams) {
        super(alertConditionId, timestamp);
        this.value = value + stringify(extraParams);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ActiveAlertConditionMessage" + "[ " + "value= " + value + ", " + super.toString() + " ]";
    }

    private String stringify(Object... extraParams) {
        if (extraParams.length == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder(", extraInfo=[");
        boolean first = true;
        for (Object extraParam : extraParams) {
            if (first) {
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append(extraParam);
        }
        builder.append("]");
        return builder.toString();
    }
}