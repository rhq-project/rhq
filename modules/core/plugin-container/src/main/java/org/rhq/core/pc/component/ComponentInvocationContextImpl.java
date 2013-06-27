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

package org.rhq.core.pc.component;

import org.rhq.core.pluginapi.component.ComponentInvocationContext;

/**
 * Plugin container implementation of {@link ComponentInvocationContext}. An instance of this class will be created
 * by the plugin container and put in the {@link org.rhq.core.pluginapi.inventory.ResourceContext}.
 *
 * Component invocation threads MUST call
 * {@link #setLocalContext(org.rhq.core.pc.component.ComponentInvocationContextImpl.LocalContext)} before executing
 * component code.
 *
 * @author Thomas Segismont
 */
public final class ComponentInvocationContextImpl implements ComponentInvocationContext {
    private static final ThreadLocal<LocalContext> localContext = new ThreadLocal<LocalContext>() {
        @Override
        protected LocalContext initialValue() {
            return new LocalContext();
        }
    };

    @Override
    public boolean isInterrupted() {
        return localContext.get().isInterrupted();
    }

    @Override
    public void markInterrupted() {
        localContext.get().markInterrupted();
    }

    /**
     * Binds the specified {@link LocalContext} instance to the current thread.
     *
     * @param localContext
     *
     * @throws IllegalArgumentException if <code>localContext</code> is null
     */
    public void setLocalContext(LocalContext localContext) {
        if (localContext == null) {
            throw new IllegalArgumentException("localContext is null");
        }
        ComponentInvocationContextImpl.localContext.set(localContext);
    }

    /**
     * Interrupted status holder. A single instance of this class should be bound to the invocation thread by calling
     * {@link #setLocalContext(org.rhq.core.pc.component.ComponentInvocationContextImpl.LocalContext)}.
     */
    public static final class LocalContext implements ComponentInvocationContext {

        private volatile boolean interrupted = false;

        @Override
        public boolean isInterrupted() {
            return interrupted;
        }

        @Override
        public void markInterrupted() {
            interrupted = true;
        }
    }
}
