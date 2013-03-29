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

package org.rhq.test.arquillian;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class TestResourceComponent implements ResourceComponent<ResourceComponent<?>> {

    private static final Log LOG = LogFactory.getLog(TestResourceComponent.class);

    private ResourceContext<?> context;

    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    @Override
    public void start(ResourceContext<ResourceComponent<?>> context) throws InvalidPluginConfigurationException,
        Exception {

        this.context = context;
    }

    @Override
    public void stop() {
        context.getDataDirectory().mkdirs();
        try {
            File f = new File(context.getDataDirectory(), "test-plugin.data");
            if (!f.exists()) {
                f.createNewFile();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Create file " + f);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not touch a marker file.");
        }
    }
}
