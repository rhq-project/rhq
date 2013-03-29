/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package org.rhq.test.arquillian;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When a test method is annotated with this annotation, the data persisted by the plugin container during its lifetime
 * can be automatically cleaned up.
 *
 * @author Lukas Krejci
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ClearPersistedData {

    /**
     * Special marker string to express the wish to affect all the plugins.
     */
    final String ALL_PLUGINS = "__ALL__";

    /**
     * The time when the clearing of the data should occur.
     * Defaults to before the invocation of the test method but can also be after or both.
     */
    When[] when() default { When.BEFORE_TEST };

    /**
     * Whether to clear the inventory.dat file (i.e. the persisted inventory).
     * Defaults to true.
     */
    boolean ofInventory() default true;

    /**
     * Whether to clear the "changesets" directory storing the drift data.
     * Defaults to true.
     */
    boolean ofDrift() default true;

    /**
     * The list of the names of the plugins to clear the data of.
     * Defaults to list containing the special {@link #ALL_PLUGINS} string that means to delete
     * the data of all plugins.
     */
    String[] ofPlugins() default { ALL_PLUGINS };
}
