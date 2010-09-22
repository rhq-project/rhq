/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.util;

/**
 * A collection of utility methods for working with Strings.
 *
 * @author Ian Springer
 */
public class StringUtility {
    public static String pluralize(String singularNoun) {
        String pluralNoun;
        if (singularNoun.endsWith("y") && !singularNoun.endsWith("ay") && !singularNoun.endsWith("ey") &&
            !singularNoun.endsWith("oy")) {
            pluralNoun = singularNoun.substring(0, singularNoun.length() - 1) + "ies";
        } else {
            pluralNoun = singularNoun + "s";
        }
        return pluralNoun;
    }

    private StringUtility() {
    }
}
