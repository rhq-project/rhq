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
package org.rhq.enterprise.server.alert.engine.model;

import org.rhq.core.domain.configuration.Configuration;

/**
 * @author Joseph Marques
 */

public class ResourceConfigurationCacheElement extends AbstractCacheElement<Configuration> {

    public ResourceConfigurationCacheElement(AlertConditionOperator operator, Configuration value,
        int conditionTriggerId) {
        super(operator, value, conditionTriggerId);
    }

    @Override
    public boolean matches(Configuration providedValue, Object... extraParams) {
        if (alertConditionOperator == AlertConditionOperator.CHANGES) {
            /*
             * as of rev1800, ConfigurationManagerBean.persistNewResourceConfigurationUpdateHistory
             * only persists configurations that have changed, thus turning this check into a no-op
             * 
             * however, let's make sure we record the latest value so that we can provide the 
             * property delta information in future enhancements to alert-configuration integration
             */
            alertConditionValue = providedValue;

            return true;
        } else {
            throw new UnsupportedAlertConditionOperatorException(getClass().getSimpleName() + " does not yet support "
                + alertConditionOperator);
        }
    }

    @Override
    public AlertConditionOperator.Type getOperatorSupportsType(AlertConditionOperator operator) {
        if (operator == AlertConditionOperator.CHANGES) {
            return operator.getDefaultType();
        }

        return AlertConditionOperator.Type.NONE;
    }

}
