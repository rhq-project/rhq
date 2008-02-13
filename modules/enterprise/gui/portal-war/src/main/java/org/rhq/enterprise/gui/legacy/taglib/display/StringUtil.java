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
package org.rhq.enterprise.gui.legacy.taglib.display;

/**
 * One line description of what this class does. More detailed class description, including examples of usage if
 * applicable.
 */

public class StringUtil extends Object {
    /**
     * Replace character at given index with the same character to upper case
     *
     * @param     oldString old string
     * @param     index     of replacement
     *
     * @return    String new string
     *
     * @exception StringIndexOutOfBoundsException &nbsp;
     */
    public static String toUpperCaseAt(String oldString, int index) throws NullPointerException,
        StringIndexOutOfBoundsException {
        int length = oldString.length();
        String newString = "";

        if ((index >= length) || (index < 0)) {
            throw new StringIndexOutOfBoundsException("Index " + index + " is out of bounds for string length "
                + length);
        }

        //get upper case replacement
        String upper = String.valueOf(oldString.charAt(index)).toUpperCase();

        //avoid index out of bounds
        String paddedString = oldString + " ";

        //get reusable parts
        String beforeIndex = paddedString.substring(0, index);
        String afterIndex = paddedString.substring(index + 1);

        //generate new String - remove padding spaces
        newString = (beforeIndex + upper + afterIndex).substring(0, length);

        return newString;
    }
}