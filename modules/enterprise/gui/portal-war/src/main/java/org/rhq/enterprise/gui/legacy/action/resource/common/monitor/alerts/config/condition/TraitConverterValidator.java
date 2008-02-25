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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config.condition;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMessage;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config.ConditionBean;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

class TraitConverterValidator implements ConditionBeanConverterValidator {
    private MeasurementDefinitionManagerLocal definitionManager;

    public TraitConverterValidator() {
        definitionManager = LookupUtil.getMeasurementDefinitionManager();
    }

    public void exportProperties(Subject subject, ConditionBean fromBean, AlertCondition toCondition) {
        MeasurementDefinition definition = definitionManager.getMeasurementDefinitionById(subject, fromBean
            .getTraitId());
        toCondition.setMeasurementDefinition(definition);

        toCondition.setCategory(AlertConditionCategory.TRAIT);
        toCondition.setName(fromBean.getTraitName());
    }

    public void importProperties(Subject subject, AlertCondition fromCondition, ConditionBean toBean) {
        toBean.setTrigger(getTriggerName());
        toBean.setTraitId(fromCondition.getMeasurementDefinition().getId());
        toBean.setTraitName(fromCondition.getName());
    }

    public boolean validate(ConditionBean bean, ActionErrors errors, int index) {
        if (bean.getTraitId() <= 0) {
            ActionMessage err = new ActionMessage("alert.config.error.NoTraitSelected");
            errors.add("condition[" + index + "].traitStatus", err);
            return false;
        }

        return true;
    }

    public String getTriggerName() {
        return "onTrait";
    }
}