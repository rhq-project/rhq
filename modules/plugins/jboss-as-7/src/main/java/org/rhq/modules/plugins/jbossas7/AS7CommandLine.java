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
import java.util.EnumSet;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.pluginapi.util.JavaCommandLine;

/**
 * Parses a JBoss AS7 command line and provides easy access to its parts.
 *
 * @author Ian Springer
 */
public class AS7CommandLine extends JavaCommandLine {

    private static final String APP_SERVER_MODULE_NAME_PREFIX = "org.jboss.as";

    private String appServerModuleName;
    private List<String> appServerArgs;

    public AS7CommandLine(String[] args) {
        // Note, we don't use EnumSet.allOf() just in case some other option delimiter is added to the enum in the future.
        super(args, true, EnumSet.of(OptionValueDelimiter.WHITESPACE, OptionValueDelimiter.EQUALS_SIGN),
                EnumSet.of(OptionValueDelimiter.WHITESPACE, OptionValueDelimiter.EQUALS_SIGN));

        // In the case of AS7, the class arguments are actually the arguments to the jboss-modules.jar main class. We
        // want to split out the arguments to the app server module (i.e. "org.jboss.as.standalone" or
        // "org.jboss.as.host-controller"). e.g. For the class arguments
        // "-mp /home/ips/Applications/jboss-as-7.1.1.Final/modules -jaxpmodule javax.xml.jaxp-provider
        // org.jboss.as.standalone -Djboss.home.dir=/opt/jboss-as-7.1.1.Final --server-config=standalone-full.xml",
        // this.appServerModuleName would get set to "org.jboss.as.standalone" and this.appServerArgs would get set to
        // "-Djboss.home.dir=/opt/jboss-as-7.1.1.Final --server-config=standalone-full.xml"
        List<String> classArgs = super.getClassArguments();
        for (int i = 0, classArgsSize = classArgs.size(); i < classArgsSize; i++) {
            String classArg = classArgs.get(i);
            if (classArg.startsWith(APP_SERVER_MODULE_NAME_PREFIX)) {
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
            throw new IllegalArgumentException("Class arguments do not contain an argument starting with \""
                    + APP_SERVER_MODULE_NAME_PREFIX + "\".");
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
