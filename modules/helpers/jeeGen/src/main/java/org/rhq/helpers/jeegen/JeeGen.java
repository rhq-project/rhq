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
        EjbArchive ejbArchive = new EjbArchive(ShrinkWrap.create(JavaArchive.class), 100, 100);
        File ejbJarFile = new File("test-ejb.jar");
        ejbArchive.as(ZipExporter.class).exportTo(ejbJarFile, true);
    }

}
