/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.helpers.pluginAnnotations.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A configuration property for resource or plugin config.
 * Currently only property simple are supported.
 * @author Heiko W. Rupp
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.FIELD})
public @interface ConfigProperty {

    public Scope scope() default Scope.PLUGIN;
    String property() default "";
    String displayName() default "";
    String description() default "";
    boolean readOnly() default false;
    String defaultValue() default "";
    RhqType rhqType() default RhqType.VOID;


    public enum Scope {
        PLUGIN,
        RESOURCE;
    }
}
