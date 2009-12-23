/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.core.pc.configuration;

import org.rhq.core.pc.util.ComponentService;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;

public interface ConfigManagement {

    static final int FACET_METHOD_TIMEOUT = 60 * 1000; // 60 seconds

    ComponentService getComponentService();

    void setComponentService(ComponentService componentService);

    ConfigurationUtilityService getConfigurationUtilityService();

    void setConfigurationUtilityService(ConfigurationUtilityService configUtilityService);

    Configuration executeLoad(int resourceId) throws PluginContainerException;

    void executeUpdate(int resourceId, Configuration configuration)
        throws PluginContainerException;

}
