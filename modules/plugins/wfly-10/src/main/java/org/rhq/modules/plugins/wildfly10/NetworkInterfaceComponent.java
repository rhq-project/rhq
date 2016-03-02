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

package org.rhq.modules.plugins.wildfly10;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;

/**
 * Component classes for network interfaces
 * @author Heiko W. Rupp
 */
public class NetworkInterfaceComponent extends BaseComponent<NetworkInterfaceComponent> implements
         ConfigurationFacet {

    private static final String[] wildCards = {"any-address"};

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {

        Configuration config = report.getConfiguration();

        boolean isWildcard = false;
        // detect if any of wildcard properties was turned on
        for (String wildcard : wildCards) {
            isWildcard |= Boolean.valueOf(config.getSimpleValue(wildcard));
        }
        if (config.getSimpleValue("inet-address") != null) {
            if (isWildcard) {
                // we couldn't know what user wants ... if setting inet-addr or one of wildcard props
                report
                    .setErrorMessage("When setting any-address to true inet-address must be unset");
                report.setStatus(ConfigurationUpdateStatus.FAILURE);
                return;
            }
            // auto-set all wildcards to undefined
            for (String wildcard : wildCards) {
                config.getSimple(wildcard).setValue(null);
            }
        } else if (!isWildcard) {
            report
                .setErrorMessage("You need to enable either any-address when inet-address is disabled");
            report.setStatus(ConfigurationUpdateStatus.FAILURE);
            return;
        }
        super.updateResourceConfiguration(report);
    }

    public static void preProcessCreateChildConfiguration(Configuration configuration) {
        for (String wildCard: wildCards) {
            PropertySimple ps = configuration.getSimple(wildCard);
            if (ps!=null && ps.getStringValue().equals("false"))
                configuration.remove(wildCard);
        }
    }
}
