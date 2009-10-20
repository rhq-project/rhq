/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.augeas.helper;

import java.util.ArrayList;
import java.util.List;

import net.augeas.Augeas;

/**
 * A collection of utility methods for working with {@link Augeas} instances.
 *
 * @author Ian Springer
 */
public class AugeasUtility {
    public static List<String> matchFilter(Augeas augeas, String expression, String value) {
        List<String> matches = new ArrayList<String>();
        List<String> paths = augeas.match(expression);
        for (String path : paths) {
            String pathValue = augeas.get(path);
            if (pathValue.equals(value)) {
                matches.add(path);
            }
        }
        return matches;
    }

    // Prevent instantiation.
    private AugeasUtility() {
    }
}
