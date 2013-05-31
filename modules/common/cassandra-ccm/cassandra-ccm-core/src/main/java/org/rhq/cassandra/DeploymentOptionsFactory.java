/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra;

/**
 * Creates {@link DeploymentOptions} objects.
 *
 * @author John Sanda
 */
public class DeploymentOptionsFactory {

    /**
     * @return A new {@link DeploymentOptions}. This method checks the platform arch
     * (32 bit vs 64 bit) and the JRE being used to determine if any particular defaults
     * need to be set. For example, on 32 bit arches running on OpenJDK 6, Cassandra
     * cannot use its default thread stack stack of 180k. It causes the JVM to segfault on
     * start up. When this runtime environment is detected, the factory will set the
     * appropriate system property so that a default thread stack of 240k is used. That
     * can be overridden by calling {@link DeploymentOptions#setStackSize(String)}.
     */
    public DeploymentOptions newDeploymentOptions() {
        String arch = System.getProperty("os.arch");
        String javaVMName = System.getProperty("java.vm.name");
        String javaVersion = System.getProperty("java.version");

        if ((arch.equals("i386") || arch.equals("amd64") || arch.equals("i686")) && javaVMName.startsWith("OpenJDK")) {
            System.setProperty("rhq.cassandra.stack.size", "240k");
        }

        return new DeploymentOptions();
    }

}
