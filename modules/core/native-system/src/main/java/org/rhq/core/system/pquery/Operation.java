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
package org.rhq.core.system.pquery;

import org.rhq.core.system.pquery.Conditional.Operator;

class Operation {
    private final Operator operator;

    Operation(Operator operator) {
        this.operator = operator;
    }

    Operator getOperator() {
        return operator;
    }

    boolean doOperation(String value1, String value2) {
        if ((value1 == null) || (value2 == null)) {
            return value1 == value2;
        }

        if (operator.equals(Operator.match)) {
            return value1.matches(value2);
        } else if (operator.equals(Operator.nomatch)) {
            return !value1.matches(value2);
        } else {
            // should never happen unless we add more operators but forgot to create a new else clause for it
            throw new IllegalArgumentException("Invalid operator: " + operator);
        }
    }
}