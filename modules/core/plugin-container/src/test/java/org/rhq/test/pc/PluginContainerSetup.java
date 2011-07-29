/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.test.pc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.rhq.core.pc.ServerServices;

/**
 * This annotation represents the setup of the plugin container to be
 * performed before each test.
 * @author Lukas Krejci
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface PluginContainerSetup {

    /**
     * The list of plugin URIs that should be loaded into the plugin container.
     * The "classpath" scheme can be used in the URI to specify that the plugin
     * is stored in the classpath of the test being executed. Other schemes are 
     * resolved as usual.
     * <p>
     * Examples of URIs:
     * <ul>
     * <li> classpath:///test-plugin.jar
     * <li> file:///some/absolute/location/on/the/filesystem.jar
     * <li> file:relative/path/to/current/directory/blah.jar
     * <li> http://www.rhq-project.org/tests/test-plugin.jar
     */
    String[] plugins();

    /**
     * If specified, the plugin container setups with the same shared group will
     * be invoked with the same directory layout for storing inventory, data and plugin
     * files. 
     * <p>
     * This enables the persistent data to survive among different plugin container
     * runs which is useful for example to test plugin upgrades. We need to test for different
     * things in each plugin container run, we need to deploy different plugins to the 
     * plugin container, yet we usually want the persisted inventory to survive.
     * <p>
     * By default each test creates a new directory layout. 
     */
    String sharedGroup() default "";
    
    /**
     * Whether the plugin container should be started and initialized before
     * the control is handed over to the test method. 
     * <p>
     * Default is false.
     */
    boolean startImmediately() default false;
    
    /**
     * If the plugin container is {@link #inAgent()}, it persists the state of 
     * the inventory in a 'inventory.dat' file. 
     * <p>
     * Because the plugin container test can start up the PC using the same 
     * directories to use (using the {@link #sharedGroup()} grouping), the persisted 
     * inventory can be shared between multiple tests.
     * <p>
     * The default value is true.  
     */
    boolean clearInventoryDat() default true;
    
    /**
     * Whether to clear the data directory of the plugin container 
     */
    boolean clearDataDir() default false;
    
    /**
     * The number of server and service discoveries executed before the control
     * is handed over to the test method. This is useful if you have deep hierarchies
     * of server resources that only get committed a level at a time.
     * <p>
     * If set to 0, no discoveries will be performed.
     * <p>
     * The default value is 1.
     */
    int numberOfInitialDiscoveries() default 1;
    
    /**
     * Should the plugin container be set to think it runs inside an agent?
     * <p>
     * The default is true. 
     */
    boolean inAgent() default true;
    
    /**
     * By default the PluginContainerTest
     * By default the {@link ServerServices} the plugin container will be initialized with
     * will contain the mocked out interfaces of the individual services.
     * <p>
     * If you need to provide custom implementations, you can tell the setup
     * to use the instance returned by the method named using this attribute.
     */
    String pluginConfigurationProviderMethod() default "";
}
