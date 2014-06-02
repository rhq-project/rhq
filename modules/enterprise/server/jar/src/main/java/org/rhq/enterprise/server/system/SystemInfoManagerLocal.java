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
package org.rhq.enterprise.server.system;

import java.util.Map;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;

/**
 * Provide system information
 * @author Heiko W. Rupp
 */
@Local
public interface SystemInfoManagerLocal {

    /**
     * Return the system information to the caller.
     * Returned content may vary depending on the caller and his rights to
     * see various details
     * @param caller User calling the method
     * @return Map of system information.
     */
    Map<String,String> getSystemInformation(Subject caller);

    /**
     * Dump the system information to the server log file
     * @param caller
     */
    void dumpToLog(Subject caller);

}
