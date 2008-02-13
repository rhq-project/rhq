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

public enum AlertConditionOperator {
    /*
     * absolute value comparison operators
     */
    LESS_THAN(Type.STATEFUL), EQUALS(Type.STATELESS), GREATER_THAN(Type.STATEFUL),

    /*
     * operator based on state deltas
     */
    CHANGES(Type.STATELESS), // state value changes in any way
    CHANGES_TO(Type.STATELESS), // becomes one specific state value
    CHANGES_FROM(Type.STATELESS); // leaves one specific state value

    private Type defaultType;

    /**
     * For the most part, the operator itself denotes whether it makes comparisons against a sliding scale or not.
     * However, this doesn't hold in every conceivable scenario. Thus, the defaultType will support 95% of the use
     * cases, and the AbstractCacheElement's getOperatorType will have a chance to override this.
     */
    AlertConditionOperator(Type defaultType) {
        this.defaultType = defaultType;
    }

    public Type getDefaultType() {
        return defaultType;
    }

    /*
     * Can only have stateful operators if we're  sure we're referring to a sliding scale
     */
    public enum Type {
        STATEFUL, STATELESS, NONE;
    }
}