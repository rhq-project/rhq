/*
 * RHQ Management Platform
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
package org.rhq.enterprise.agent;

/**
 * Updates Java Service Wrapper environment files (e.g. *.env files)
 * 
 * @author John Mazzitelli
 */
public class JavaServiceWrapperEnvironmentScriptFileUpdate extends EnvironmentScriptFileUpdate {

    /**
     * Constructor given the full path to config file.
     *
     * @param location location of the file
     */
    public JavaServiceWrapperEnvironmentScriptFileUpdate(String location) {
        super(location);
    }

    @Override
    protected String createEnvironmentVariableLine(NameValuePair nvp) {
        return "set." + nvp.name + "=" + nvp.value;
    }

    @Override
    protected NameValuePair parseEnvironmentVariableLine(String line) {
        // it can't match what we are looking for if it doesn't have an "=" in it
        if (line == null || !line.contains("=")) {
            return null;
        }

        String trimmed = line.trim();

        // ignore commented out lines
        if (trimmed.startsWith("#")) {
            return null;
        }

        // JSW env scripts set environment variables in the form "set.VAR=NAME"
        // we need to strip off the "set." which leaves us with VAR=NAME
        if (!trimmed.startsWith("set.")) {
            return null;
        }

        trimmed = trimmed.substring(4);

        String[] nameValue = trimmed.split("=", 2);
        return new NameValuePair(nameValue[0], nameValue[1]);
    }
}