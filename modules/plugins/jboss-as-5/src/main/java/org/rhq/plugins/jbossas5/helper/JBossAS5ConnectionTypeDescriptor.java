/*
 * Jopr Management Platform
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
package org.rhq.plugins.jbossas5.helper;

import org.mc4j.ems.connection.support.metadata.JBossConnectionTypeDescriptor;

/**
 * A connection type descriptor to support jnp connections to JBoss AS 5 through EMS.
 * 
 * @author Lukas Krejci
 */
public class JBossAS5ConnectionTypeDescriptor extends JBossConnectionTypeDescriptor {

    private static final long serialVersionUID = 1L;

    @Override
    public String getRecongnitionPath() {
        return "lib/jboss-main.jar";
    }

    @Override
    public String[] getConnectionClasspathEntries() {
        //        This is what Scott recommended using for JBAS 5.1:
        //
        //            <classpath>
        //                    <classpathentry kind="src" path="src"/>
        //                    <classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
        //                    <classpathentry kind="var" path="JBOSS/client/jboss-common-core.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/jnp-client.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/lib/jboss-profileservice-spi.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/jboss-logging-jdk.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/jboss-logging-log4j.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/log4j.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/jboss-logging-spi.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/jboss-aop-client.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/jboss-remoting.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/common/lib/jboss-security-aspects.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/jboss-aspect-jdk50-client.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/trove.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/javassist.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/jboss-mdr.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/jboss-security-spi.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/jbosssx-client.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/jboss-javaee.jar"/>
        //                    <classpathentry kind="var" path="JBOSS/client/concurrent.jar"/>
        //                    <classpathentry kind="output" path="bin"/>
        //            </classpath>

        return new String[] { "client/jboss-common-core.jar", "client/jnp-client.jar",
            "lib/jboss-profileservice-spi.jar", "client/jboss-logging-jdk.jar", "client/jboss-logging-jdk.jar",
            "client/log4j.jar", "client/jboss-logging-spi.jar", "client/jboss-aop-client.jar",
            "client/jboss-remoting.jar", "common/lib/jboss-security-aspects.jar",
            "client/jboss-aspect-jdk50-client.jar", "client/trove.jar", "client/javassist.jar", "client/jboss-mdr.jar",
            "client/jboss-security-spi.jar", "client/jbosssx-client.jar", "client/jboss-javaee.jar",
            "client/concurrent.jar", "client/jmx-invoker-adaptor-client.jar", "client/jboss-client.jar",
            "client/jboss-integration.jar", "client/jboss-serialization.jar",
            //this is to support hibernate plugin out of the box in jboss as 5
            "common/lib/hibernate-core.jar" };
    }

}
