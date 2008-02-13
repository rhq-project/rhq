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
package org.rhq.enterprise.gui.inventory.group;

import org.jetbrains.annotations.Nullable;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.group.AggregatePluginConfigurationUpdate;
import org.rhq.core.gui.configuration.ConfigurationMaskingUtility;
import org.rhq.core.gui.util.FacesContextUtility;

public class ViewGroupConnectionPropertyDetailsHeaderUIBean extends ViewGroupConnectionPropertiesUIBean {
    public static final String MANAGED_BEAN_NAME = "ViewGroupConnectionPropertyDetailsHeaderUIBean";

    @Nullable
    @Override
    protected Configuration lookupConfiguration() {
        Configuration aggregateConfiguration = null;

        try {
            int updateId = FacesContextUtility.getRequiredRequestParameter("apcuId", Integer.class);
            AggregatePluginConfigurationUpdate update = configurationManager
                .getAggregatePluginConfigurationById(updateId);
            aggregateConfiguration = update.getConfiguration();

            if (aggregateConfiguration != null) {
                ConfigurationMaskingUtility.maskConfiguration(aggregateConfiguration, this.configurationDefinition);
            }
        } catch (IllegalArgumentException iae) {
            // do nothing, let null bubble up so that this object finishes constructing itself
        }

        return aggregateConfiguration;
    }
}