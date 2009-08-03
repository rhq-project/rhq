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
package org.rhq.enterprise.client.script;

import java.util.List;
import java.util.LinkedList;

public class ScriptCmdLine {
    public ScriptCmdLine() {
    }

    public static enum ArgType {
        INDEXED("indexed"),

        NAMED("named");

        private String value;

        private ArgType(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    private String scriptFileName;

    private ArgType argType = ArgType.INDEXED;

    private List<ScriptArg> args = new LinkedList<ScriptArg>();

    public String getScriptFileName() {
        return scriptFileName;
    }

    public void setScriptFileName(String scriptFileName) {
        this.scriptFileName = scriptFileName;
    }

    public ArgType getArgType() {
        return argType;
    }

    public void setArgType(ArgType argType) {
        this.argType = argType;
    }

    public List<ScriptArg> getArgs() {
        return args;
    }

    public void addArg(ScriptArg arg) {
        args.add(arg);
    }

}
