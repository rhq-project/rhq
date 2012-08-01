/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.plugins.hadoop;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;

/**
 * Handles performing operations on Hadoop node instance.
 * 
 * @author Jirka Kremser
 */
public class HadoopOperationsDelegate {

    private ResourceContext<HadoopServiceComponent> resourceContext;

    public HadoopOperationsDelegate(ResourceContext<HadoopServiceComponent> resourceContext) {
        this.resourceContext = resourceContext;
    }

    public OperationResult invoke(HadoopSupportedOperations operation, Configuration parameters)
        throws InterruptedException {

        String message = null;
        switch (operation) {
        case FORMAT:
            message = format();
            break;
        default:
            throw new UnsupportedOperationException(operation.toString());
        }
        OperationResult result = new OperationResult(message);
        return result;
    }

    /**
     * Format a new distributed filesystem
     * by running $bin/hadoop namenode -format
     * 
     * @return message
     */
    private String format() {
        String hadoopHome = resourceContext.getPluginConfiguration()
            .getSimple(HadoopServiceDiscovery.HOME_DIR_PROPERTY).getStringValue();
        return null;
    }
}
