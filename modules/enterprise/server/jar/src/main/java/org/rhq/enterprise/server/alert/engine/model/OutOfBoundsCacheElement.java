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
package org.rhq.enterprise.server.alert.engine.model;

import org.rhq.enterprise.server.alert.engine.model.AlertConditionOperator.Type;

/**
 * @author Joseph Marques
 */

public final class OutOfBoundsCacheElement extends NumericDoubleCacheElement {
    public OutOfBoundsCacheElement(AlertConditionOperator operator, Double value, int scheduleId) {
        super(operator, value, scheduleId);
    }

    @Override
    public Type getOperatorSupportsType(AlertConditionOperator operator) {
        /*
         * all (e.g., the only two) supported OOB operators are STATELESS, because only alertCondition types currently
         * support downstream processing (which require STATEFUL operator support for proper operation)
         */
        if ((operator == AlertConditionOperator.GREATER_THAN) || (operator == AlertConditionOperator.LESS_THAN)) {
            return AlertConditionOperator.Type.STATELESS;
        }

        return AlertConditionOperator.Type.NONE;
    }

    @Override
    public boolean matches(Double value, Object extraParams) {
        /*
         * dynamic measurements that often report at- or near-zero values, will end up having at- or near-zero baselines
         * created against them.  consequently, both high and low OOBs trigger because they're both within the 1e-9
         * threshold that the NumericDoubleCacheElement uses for its double comparisons.
         *
         * the remedy is to suppress OOBs that have at- or near-zero associated baselines.  this could have been done by
         * fixing the AlertConditionCacheManagerBean code to prevent at- or near-zero insertions, as well as removing
         * elements during update- processing when the new value is at- or near-zero.  however, a simpler solution is to
         * allow the occasional offender into the cache, but always have the match processing return false in these
         * circumstances.
         */
        if (Math.abs(alertConditionValue) < 1e-9) {
            return false;
        }

        /*
         * if the OOB does NOT have an at- or near-zero alertConditionValue, default to regular threshold comparison as
         * defined in NumericDoubleCacheElement
         */
        return super.matches(value, extraParams);
    }
}