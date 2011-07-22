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

package org.rhq.enterprise.server.sync;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.sync.ExportReport;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Remote
public interface SynchronizationManagerRemote {

    /**
     * This exports the configuration data of all supported subsystems in RHQ.
     * <p>
     * The returned report contains the data bytes themselves as well as a map of 
     * messages and notes produced by the different subsystem exporters so that
     * the caller of this method is able to determine possible problems of the export
     * file without needing to deserialize and read it (the same messages are also included
     * in the export data).
     * <p>
     * The export data is a zipped XML.
     * <p>
     * The export requires the user to have {@link Permission#MANAGE_INVENTORY} permission.
     * 
     * @param subject the logged in user
     * @return the export report
     */
    ExportReport exportAllSubsystems(Subject subject);
}
