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

/**
 * Represents a single criteria in a process info query string. A <i>criteria</i> consists of a left and right hand side
 * of an equals sign, with the left side consisting of the <i>conditional</i> and the right hand side being the <i>
 * value</i>.
 *
 * @author John Mazzitelli
 */
class Criteria {
    private final Conditional conditional;
    private final String value;

    Criteria(String criteria) {
        String[] tokens = criteria.split("=", 2);

        if (tokens.length != 2) {
            throw new IllegalArgumentException("Criteria needs a conditional and a value: " + criteria);
        }

        conditional = new Conditional(tokens[0]);
        value = tokens[1];

        return;
    }

    public Conditional getConditional() {
        return conditional;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        return conditional + "=" + value;
    }
}