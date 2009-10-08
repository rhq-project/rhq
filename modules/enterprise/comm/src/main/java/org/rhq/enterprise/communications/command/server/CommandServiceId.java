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
package org.rhq.enterprise.communications.command.server;

import java.io.Serializable;
import org.rhq.enterprise.communications.ServiceContainer;

/**
 * This class identifies a registered command service.
 *
 * @author John Mazzitelli
 * @see    ServiceContainer#removeCommandService(CommandServiceId)
 * @see    ServiceContainer#addCommandService(CommandService)
 */
public class CommandServiceId implements Serializable {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * The actual ID.
     */
    private String m_id;

    /**
     * Constructor for {@link CommandServiceId}.
     *
     * @param  id the ID (must not be <code>null</code>)
     *
     * @throws IllegalArgumentException if <code>id</code> is <code>null</code>
     */
    public CommandServiceId(String id) throws IllegalArgumentException {
        if (id == null) {
            throw new IllegalArgumentException("id=null");
        }

        m_id = id;
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (obj instanceof CommandServiceId) {
            return m_id.equals(((CommandServiceId) obj).m_id);
        }

        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return m_id.hashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return m_id;
    }
}