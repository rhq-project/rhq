/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.test.arquillian;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Instructs to run discovery before a test method is executed.
 *
 * @author Lukas Krejci
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RunDiscovery {
    /**
     * True (the default) if server discovery should be run. 
     */
    boolean discoverServers() default true;
    
    /**
     * True (the default) if service discovery should be run.
     */
    boolean discoverServices() default true;
    
    /**
     * Using this attribute you can limit the number of times the discovery is
     * run. If the plugin container runs in embedded mode, the value of this attribute
     * isn't taken into account, because in embedded mode, the whole resource hierarchy
     * is discovered every time (to be precise, the resources are auto-committed which
     * will cause the discovery to descend into child resources automatically). 
     * <p>
     * If you run the plugin container in the agent mode, the number of times the discovery
     * should be run is determined by the depth of the resource type hierarchy. This assumes
     * that you commit the resources into an inventory in your server-side (mocked) 
     * implementation (which is by default a sensible thing to do).
     * <p>
     * If you don't do that and you want to limit the number of times the discovery runs
     * in the plugin container (to a value lower than the depth of resource type hierarchy),
     * you can use this attribute.
     * <p>
     * In embedded mode, the discovery is always run only once. The default behavior in the
     * agent mode is to base the number of discovery runs on the depth of the resource type
     * hierarchy (which is designated by a negative value of this attribute).
     * <p>
     * A non-negative value of this attribute will specify the exact number of times the discovery
     * will be run in agent mode.
     * <p>
     * Essentially, you should use this attribute only if you know what you are doing :)
     */
    int numberOfTimes() default -1;
}
