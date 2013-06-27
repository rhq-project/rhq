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

package org.rhq.core.pluginapi.component;

/**
 * An instance of this class is created by the plugin container and bound to facet-locked component invocation
 * thread.
 * 
 * It helps to detect interrupted component invocations (canceled or timed out).
 *
 * @author Thomas Segismont
 */
public interface ComponentInvocationContext {

    /**
     * Whether the component invocation has been interrupted or not.
     * 
     * @return true if component invocation has been interrupted, false otherwise.
     */
    boolean isInterrupted();


    /**
     * Mark this context as interrupted.
     */
    void markInterrupted();
}
