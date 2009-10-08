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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMessage;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config.ConditionBean;

public class EventsConverterValidator implements ConditionBeanConverterValidator {
    public void exportProperties(Subject subject, ConditionBean fromBean, AlertCondition toCondition) {
        toCondition.setCategory(AlertConditionCategory.EVENT);
        toCondition.setName(fromBean.getEventSeverity());
        toCondition.setOption(fromBean.getEventDetails());
    }

    public void importProperties(Subject subject, AlertCondition fromCondition, ConditionBean toBean) {
        toBean.setTrigger(getTriggerName());
        toBean.setEventSeverity(fromCondition.getName());
        toBean.setEventDetails(fromCondition.getOption());
    }

    public boolean validate(ConditionBean bean, ActionErrors errors, int index) {
        if (0 == bean.getEventSeverity().length()) {
            ActionMessage err = new ActionMessage("alert.config.error.NoEventSeveritySelected");
            errors.add("condition[" + index + "].eventSeverity", err);
            return false;
        }

        if (null != bean.getEventDetails() && 0 != bean.getEventDetails().length()) {
            try {
                Pattern.compile(bean.getEventDetails());
            } catch (PatternSyntaxException pse) {
                ActionMessage err = new ActionMessage("alert.config.error.InvalidEventDetails");
                errors.add("condition[" + index + "].eventDetails", err);
                return false;
            }
        }

        return true;
    }

    public String getTriggerName() {
        return "onEvent";
    }
}