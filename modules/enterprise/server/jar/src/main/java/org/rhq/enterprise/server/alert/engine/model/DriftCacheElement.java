/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

public class DriftCacheElement extends AbstractCacheElement<Object> {

    public DriftCacheElement(AlertConditionOperator operator, int conditionTriggerId) {
        super(operator, "", conditionTriggerId);
    }

    @Override
    public boolean matches(Object providedValue, Object... extraParams) {
        if (alertConditionOperator == AlertConditionOperator.CHANGES) {
            return true; // any drift report we get implies the content has changed
        } else {
            throw new UnsupportedAlertConditionOperatorException(getClass().getSimpleName() + " does not support "
                + alertConditionOperator);
        }
    }

    @Override
    public AlertConditionOperator.Type getOperatorSupportsType(AlertConditionOperator operator) {
        if (operator == AlertConditionOperator.CHANGES) {
            return operator.getDefaultType();
        }

        return AlertConditionOperator.Type.NONE;
    }
}
