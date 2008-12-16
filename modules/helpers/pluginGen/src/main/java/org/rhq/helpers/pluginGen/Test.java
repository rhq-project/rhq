/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.helpers.pluginGen;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple test class to easily trigger creation of output
 *
 * @author Heiko W. Rupp
 */
public class Test {

    private final Log log = LogFactory.getLog(Test.class);

    public static void main(String[] args) throws Exception {

        File f = new File(".");
        System.out.println("here: " + f.getAbsolutePath());

        Props p = new Props();
        p.setName("foo");
        p.setCategory(Props.ResourceCategory.SERVICE);
        p.setPackagePrefix("com.acme.plugin");
        p.setDiscoveryClass("FooDiscovery");
        p.setComponentClass("FooComponent");
        p.setResourceConfiguration(true);
        p.setMonitoring(true);
        p.setOperations(true);
        p.setEvents(true);
        p.setCreateChildren(true);
        p.setSingleton(true);


        PluginGen pg = new PluginGen();
        pg.createFile(p,"descriptor","rhq-plugin.xml","/tmp");
        pg.createFile(p,"component", "FooComponent.java", "/tmp");
        pg.createFile(p,"discovery", "FooDiscovery.java", "/tmp");
        pg.createFile(p,"pom", "pom.xml", "/tmp");
        pg.createFile(p,"eventPoller", "DummyEventPoller.java", "/tmp");

    }
}
