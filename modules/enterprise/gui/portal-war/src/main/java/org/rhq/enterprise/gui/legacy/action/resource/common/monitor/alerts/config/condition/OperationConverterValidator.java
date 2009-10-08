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
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config.ConditionBean;

public class OperationConverterValidator implements ConditionBeanConverterValidator {
    public void exportProperties(Subject subject, ConditionBean fromBean, AlertCondition toCondition) {
        toCondition.setCategory(AlertConditionCategory.CONTROL);
        toCondition.setName(fromBean.getControlAction());
        toCondition.setOption(fromBean.getControlActionStatus());
    }

    public void importProperties(Subject subject, AlertCondition fromCondition, ConditionBean toBean) {
        toBean.setTrigger(getTriggerName());
        toBean.setControlAction(fromCondition.getName());
        toBean.setControlActionStatus(fromCondition.getOption());
    }

    public boolean validate(ConditionBean bean, ActionErrors errors, int index) {
        if (0 == bean.getControlAction().length()) {
            ActionMessage err = new ActionMessage("alert.config.error.NoControlActionSelected");
            errors.add("condition[" + index + "].controlAction", err);
            return false;
        }

        if (0 == bean.getControlActionStatus().length()) {
            ActionMessage err = new ActionMessage("alert.config.error.NoControlActionStatusSelected");
            errors.add("condition[" + index + "].controlActionStatus", err);
            return false;
        }

        return true;
    }

    public String getTriggerName() {
        return "onOperation";
    }
}