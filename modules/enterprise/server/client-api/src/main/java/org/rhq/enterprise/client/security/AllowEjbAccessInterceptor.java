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

package org.rhq.enterprise.client.security;

import java.security.AccessController;
import java.security.Permission;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class AllowEjbAccessInterceptor {

    private static final Permission PERM = new AllowEjbAccessPermission();

    @AroundInvoke
    public Object intercept(InvocationContext invocationContext) throws Exception {
        //check that the caller has permissions to access the EJBs.
        //normal code does, only alert CLI scripts that try to circumvent our
        //manager proxies don't.
        AccessController.checkPermission(PERM);
        return invocationContext.proceed();
    }
}
