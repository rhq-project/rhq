/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.helpers.jeegen;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.rhq.helpers.jeegen.ejb.EjbArchive;

/**
 * A tool to generate JEE test applications.
 *
 * @author Ian Springer
 */
public class JeeGen {

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            printUsageAndExit();
        }

        String ejbVersion = args[0];
        int entityBeanCount = 0;
        int statelessSessionBeanCount = 0;
        int statefulSessionBeanCount = 0;
        int messageDrivenBeanCount = 0;
        try {
            entityBeanCount = Integer.parseInt(args[1]);
            statelessSessionBeanCount = Integer.parseInt(args[2]);
            statefulSessionBeanCount = Integer.parseInt(args[3]);
            messageDrivenBeanCount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            printUsageAndExit();
        }

        if (entityBeanCount < 0 || statelessSessionBeanCount < 0 || statefulSessionBeanCount < 0 || messageDrivenBeanCount < 0) {
            printUsageAndExit();
        }

        EjbArchive ejbArchive = new EjbArchive(ShrinkWrap.create(JavaArchive.class), ejbVersion, entityBeanCount,
            statelessSessionBeanCount, statefulSessionBeanCount, messageDrivenBeanCount);
        File ejbJarFile = new File("test-ejb.jar");
        ejbArchive.as(ZipExporter.class).exportTo(ejbJarFile, true);
    }

    private static void printUsageAndExit() {
        System.err.println("Usage: " + JeeGen.class.getName() +
            " EJB_VERSION ENTITY_BEAN_COUNT STATELESS_SESSION_BEAN_COUNT STATEFUL_SESSION_BEAN_COUNT MESSAGE_DRIVEN_BEAN_COUNT");
        System.err.println("Example: " + JeeGen.class.getName() + " 3.0 10 10 10 10");
        System.exit(1);
    }

}
