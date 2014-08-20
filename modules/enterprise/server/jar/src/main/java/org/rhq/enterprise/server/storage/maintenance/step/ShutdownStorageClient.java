/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.storage.maintenance.step;

import org.rhq.enterprise.server.storage.maintenance.MaintenanceStepRunner;
import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;


/**
 * @author Stefan Negrea
 *
 */
public class ShutdownStorageClient extends BaseStepRunner implements MaintenanceStepRunner {

    @Override
    public void execute() {
        storageClientManager.shutdown();
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return null;
    }
}
