/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.alert.description;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Describes <code>CONTROL </code> {@link AlertCondition}s.
 *
 * @author Justin Harris
 */
public class ControlDescriber extends AlertConditionDescriber {

    @Override
    public AlertConditionCategory[] getDescribedCategories() {
        return makeCategories(AlertConditionCategory.CONTROL);
    }

    @Override
    public void createDescription(AlertCondition condition, StringBuilder builder) {
        OperationDefinition definition = getDefinition(condition);

        if (definition != null) {
            builder.append(definition.getDisplayName());
        }

        builder.append(' ');
        builder.append(definition.getDisplayName());
        builder.append(' ');
        builder.append(condition.getOption());
    }

    private OperationDefinition getDefinition(AlertCondition condition) {
        OperationManagerLocal operationManager = LookupUtil.getOperationManager();

        Integer resourceTypeId = condition.getAlertDefinition().getResource().getResourceType().getId();
        String operationName = condition.getName();

        try {
            return operationManager.getOperationDefinitionByResourceTypeAndName(
                resourceTypeId, operationName, false);
        } catch (Exception e) {
            return null;
        }
    }
}