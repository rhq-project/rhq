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
package org.rhq.enterprise.server.authz;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.rhq.core.domain.authz.Permission;

/**
 * This annotation defines a global JON permission that is required in order to invoke a particular SLSB method.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = { ElementType.METHOD })
public @interface RequiredPermission {
    /**
     * A global JON permissions (i.e. permission where
     * <code>permission.getTarget() == Permission.Target.GLOBAL</code>).
     */
    public Permission value();

    /**
     * If a user fails the required permissions check, this will be the error message
     * of the exception that gets thrown.  If this is not specified, it defaults to
     * an empty string which will be interpreted by the interceptor that performs
     * the security checks that a default, generic error message is to be used.
     */
    public String error() default "";
}