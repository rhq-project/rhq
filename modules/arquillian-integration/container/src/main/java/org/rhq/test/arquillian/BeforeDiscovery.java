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
 * Marks a method as to be run before the discovery is executed
 * on a test method.
 *
 * @author Lukas Krejci
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BeforeDiscovery {

    /**
     * This list of test methods the method annotated with this annotation applies to. 
     * <p>
     * By default the {@code @BeforeDiscovery} method applies to all test methods.
     */
    String[] testMethods() default {};

    /**
     * If order of the {@code @BeforeDiscovery} methods is significant, you can order them
     * by using this parameter.
     * <p>
     * Zero or negative value means no significant order required.
     */
    int order() default 0;
}
