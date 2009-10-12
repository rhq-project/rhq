 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.jbossas.util;

/**
 * @author Mark Spritzler
 */
public class FileNameUtility {
    /**
     * try to weed out characters which are not suitable for inclusion in a filename. It will replace all characters
     * which are not letters, digits and part of the basic latin character set with underscores.
     *
     * @param  toBeFormatted
     *
     * @return formattedFileName
     */
    public static String formatFileName(String toBeFormatted) {
        if ((toBeFormatted == null) || "".equals(toBeFormatted)) {
            return toBeFormatted;
        }

        String lowered = toBeFormatted.toLowerCase();
        char[] characters = lowered.toCharArray();
        char[] formattedCharacters = new char[characters.length];
        for (int i = 0; i < characters.length; i++) {
            char c = characters[i];
            if (!Character.isLetterOrDigit(c) || !(Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN)) {
                formattedCharacters[i] = '_';
            } else {
                formattedCharacters[i] = c;
            }
        }

        return String.valueOf(formattedCharacters);
    }
}