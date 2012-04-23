/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.core.pluginapi.util;

/**
 * The name(s) and other metadata associated with a command line option.
 *
 * @author Ian Springer
 */
public class CommandLineOption {

    private String shortName;
    private String longName;
    private boolean expectsValue;

    /**
     * Same as <code>JavaCommandLineOption(shortName, longName, true)</code> 
     */
    public CommandLineOption(char shortName, String longName) {
        this(new String(new char[] { shortName }), longName, true);
    }

    public CommandLineOption(char shortName, String longName, boolean expectsValue) {
        this(new String(new char[] { shortName }), longName, expectsValue);
    }

    /**
     * Same as <code>JavaCommandLineOption(shortName, longName, true)</code> 
     */
    public CommandLineOption(String shortName, String longName) {
        this(shortName, longName, true);
    }

    public CommandLineOption(String shortName, String longName, boolean expectsValue) {
        if ((shortName == null) && (longName == null)) {
            throw new IllegalArgumentException("ShortName and longName cannot both be null.");
        }

        this.shortName = shortName;
        this.longName = longName;
        this.expectsValue = expectsValue;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    public boolean isExpectsValue() {
        return expectsValue;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        if (this.shortName != null) {
            buffer.append('-').append(this.shortName);
            if (this.expectsValue) {
                buffer.append("=VALUE");
            }
            if (this.longName != null) {
                buffer.append('|');
            }
        }
        if (this.longName != null) {
            buffer.append("--").append(this.longName);
            if (this.expectsValue) {
                buffer.append("=VALUE");
            }
        }
        return buffer.toString();
    }

}
