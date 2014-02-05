/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.jbossas7.helper;

/**
* @author Thomas Segismont
*/
public enum TrustStrategy {
    STANDARD("standard"), TRUST_SELFSIGNED("trustSelfsigned"), TRUST_ANY("trustAny");

    public final String name;

    TrustStrategy(String name) {
        this.name = name;
    }

    public static TrustStrategy findByName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        for (TrustStrategy strategy : values()) {
            if (strategy.name.equals(name)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("No constant with name: " + name);
    }
}
