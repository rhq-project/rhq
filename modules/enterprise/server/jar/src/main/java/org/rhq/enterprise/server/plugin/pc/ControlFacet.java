/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugin.pc;

import org.rhq.core.domain.configuration.Configuration;

/**
 * Interface that server plugin components must implement if they want to expose
 * control operations. These controls allow users to invoke plugin component
 * operations at runtime.
 * 
 * @author John Mazzitelli
 */
public interface ControlFacet {
    /**
     * Plugin components must implement this method to allow its control operations to be invoked.
     * Note that implementations should not throw exceptions. If an invocation failed, the implementation
     * must catch the exceptions and indicate the failure in the {@link ControlResults#getError()} message.
     *
     * @param name the name of the control operation to be invoked
     * @param parameters the parameters being passed into the control operation (may be <code>null</code> if
     *                   no parameters were defined for the named control operation)
     * @return the results of the invocation, must never be <code>null</code>
     */
    ControlResults invoke(String name, Configuration parameters);
}
