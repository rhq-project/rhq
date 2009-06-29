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

package org.rhq.gui.webdav;

/**
 * Seems like some of our resource names aren't able to safely become URLs that contain characters
 * like \ or /. This utility will attempt to change names so they can safely be handled.
 * 
 * @author John Mazzitelli
 */
public abstract class SafeEncoder {
    // pick things that are at least half-sane, even if they doesn't look perfect
    private static final String[][] REPLACEMENTS = new String[][] { { "\\", "||" }, { "/", "|" } };

    public static String encode(String name) {
        for (String[] replacement : REPLACEMENTS) {
            name = name.replace(replacement[0], replacement[1]);
        }
        return name;
    }

    public static String decode(String name) {
        for (String[] replacement : REPLACEMENTS) {
            name = name.replace(replacement[1], replacement[0]);
        }
        return name;
    }
}
