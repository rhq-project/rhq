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
package org.rhq.enterprise.server.system;

import java.util.Properties;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.common.ServerDetails;
import org.rhq.core.domain.common.composite.SystemSettings;

/**
 * @author John Mazzitelli
 */

@Remote
public interface SystemManagerRemote {
    /**
     * Provides product information suitable for "About" details.
     *
     * @param subject user making the request
     *
     * @return the product info
     */
    ProductInfo getProductInfo(Subject subject);

    /**
     * Provides details (such as product version) of the server processing the request.  Requires MANAGE_SETTINGS.
     *
     * @param subject user making the request
     *
     * @return server details
     */
    ServerDetails getServerDetails(Subject subject);

    /**
     * @param subject
     * @return system config
     * @deprecated use {@link #getSystemSettings(Subject)} instead
     */
    @Deprecated
    Properties getSystemConfiguration(Subject subject);

    /**
     * Get the server cloud configuration. These are the server configurations that will be
     * the same for all servers in the HA server cloud.
     * <p/>
     * Note that any password fields in the returned settings will be masked (i.e. will not correspond to the actual
     * value stored in the database), see {@link org.rhq.core.domain.configuration.PropertySimple#MASKED_VALUE}.
     *
     * @param subject user making the request
     *
     * @return the settings
     */
    SystemSettings getSystemSettings(Subject subject);

    /**
     * @param subject
     * @param properties
     * @param skipValidation
     * @throws Exception
     * @deprecated use {@link #setSystemSettings(Subject, SystemSettings)} instead
     */
    @Deprecated
    void setSystemConfiguration(Subject subject, Properties properties, boolean skipValidation) throws Exception;

    /**
     * Set the server cloud configuration.  The given properties will be the new settings
     * for all servers in the HA server cloud.
     *
     * @param subject        the user who wants to change the settings
     * @param settings     the new system configuration settings
     * @throws Exception
     */
    void setSystemSettings(Subject subject, SystemSettings settings) throws Exception;

    /**
     * Performs some reconfiguration things on the server where we are running.
     * This includes redeploying the configured JAAS modules.
     */
    void reconfigureSystem(Subject whoami);
}
