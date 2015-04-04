/*
 * RHQ Management Platform
 * Copyright (C) 2005-2015 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.plugins.server;

import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * Overrides the behavior of JMX plugin's updateResourceConfiguration. We can't set readOnly properties,
 * as they're meant to be readOnly.
 *
 * @author Michael Burman
 */
public class StorageClientResourceComponent extends MBeanResourceComponent {

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        super.updateResourceConfiguration(report, true);
    }
}
