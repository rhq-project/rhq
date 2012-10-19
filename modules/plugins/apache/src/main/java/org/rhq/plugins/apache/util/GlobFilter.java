/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.plugins.apache.util;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

/**
 * A filter implementing a glob pattern match.
 * 
 * @author Lukas Krejci
 */
public class GlobFilter implements FileFilter {

    private String globPattern;
    private Pattern regexPattern;

    public static final char[] WILDCARD_CHARS;

    static {
        if (File.separatorChar == '\\') {
            WILDCARD_CHARS = new char[] { '*', '?' };
        } else {
            WILDCARD_CHARS = new char[] { '*', '?', '[', ']' };
        }
    }

    public GlobFilter(String globPattern) {
        if (globPattern == null) {
            throw new IllegalArgumentException("The glob pattern cannot be null.");
        }

        this.globPattern = globPattern;
        this.regexPattern = convert(globPattern);
    }

    public String getGlobPattern() {
        return globPattern;
    }

    /* (non-Javadoc)
     * @see java.io.FileFilter#accept(java.io.File)
     */
    public boolean accept(File pathname) {
        return regexPattern.matcher(pathname.getAbsolutePath()).matches();
    }

    private static Pattern convert(String globPattern) {
        StringBuilder regexPattern = new StringBuilder("^");
        int i = 0;
        //path starts require special handling only on UNIX platforms
        boolean pathStart = File.separatorChar != '\\';
        boolean inRangeSpec = false;
        
        while (i < globPattern.length()) {
            switch (globPattern.charAt(i)) {
            case '\\':
                if (File.separatorChar == '\\') {
                    //we're on windows, \ is a separator
                    regexPattern.append("\\\\");
                } else {
                    //anywhere else, \ is a escape sequence
                    if (i == globPattern.length() - 1) {
                        throw new IllegalArgumentException("Illegal glob pattern: " + globPattern);
                    }
                    regexPattern.append("\\").append(globPattern.charAt(i + 1));
                    i += 1; //just skip the next character
                }
                pathStart = false;
                break;
            case '*':
                if (pathStart) {
                    //on UNIX platforms, the "/*" doesn't match
                    //the hidden files (i.e. the ones prefixed by dot
                    regexPattern.append("($|[^\\.].*)");
                } else if (inRangeSpec) {
                    //* has no special meaning inside a range spec
                    regexPattern.append("\\*");
                } else {
                    regexPattern.append(".*");
                }
                pathStart = false;
                break;
            case '?':
                if (inRangeSpec) {
                    //? has no special meaning inside a range spec
                    regexPattern.append("\\?");
                } else {
                    regexPattern.append(".");
                }
                pathStart = false;
                break;
            case '.':
                regexPattern.append("\\.");
                pathStart = false;
                break;
            case '/':
                regexPattern.append("\\/");
                if (File.separatorChar != '\\') {
                    pathStart = true;
                }
                break;
            case '[':
                regexPattern.append("[");
                inRangeSpec = true;
                break;
            case ']':
                regexPattern.append(']');
                inRangeSpec = false;
                break;
            default:
                regexPattern.append(globPattern.charAt(i));
                pathStart = false;
                break;
            }
            i++;
        }

        regexPattern.append("$");

        return Pattern.compile(regexPattern.toString());
    }
}
