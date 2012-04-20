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
package org.rhq.modules.plugins.jbossas7;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.pluginapi.util.JavaCommandLine;

/**
 * Parses a JBoss AS7 command line and provides easy access to its parts.
 *
 * @author Ian Springer
 */
public class AS7CommandLine extends JavaCommandLine {

    private String appServerModuleName;
    private List<String> appServerArgs;

    public AS7CommandLine(String[] args) {
        super(args, true, OptionFormat.SPACE_OR_EQUALS, OptionFormat.POSIX);

        List<String> classArgs = super.getClassArguments();
        // The class arguments are actually the arguments to the jboss-modules.jar main class. We want to get to the
        // arguments to the app server module (i.e. "org.jboss.as.standalone" or "org.jboss.as.host-controller").
        // e.g. "-mp /home/ips/Applications/jboss-as-7.1.1.Final/modules -jaxpmodule javax.xml.jaxp-provider
        //       org.jboss.as.standalone -Djboss.home.dir=/opt/jboss-as-7.1.1.Final --server-config=standalone-full.xml"
        // In
        for (int i = 0, classArgsSize = classArgs.size(); i < classArgsSize; i++) {
            String classArg = classArgs.get(i);
            if (classArg.startsWith("org.jboss.as")) {
                this.appServerModuleName = classArg;
                if ((i + 1) < classArgsSize) {
                    this.appServerArgs = Collections.unmodifiableList(classArgs.subList(i + 1, classArgsSize));
                } else {
                    this.appServerArgs = Collections.emptyList();
                }
                break;
            }
        }
        if (this.appServerModuleName == null) {
            throw new IllegalArgumentException("Class arguments do not contain an argument starting with \"org.jboss.as\".");
        }
    }

    @NotNull
    public String getAppServerModuleName() {
        return this.appServerModuleName;
    }

    @NotNull
    public List<String> getAppServerArguments() {
        return this.appServerArgs;
    }

}
