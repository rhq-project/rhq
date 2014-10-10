/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.core.domain.alert.builder.condition;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.event.EventSeverity;

/**
 * @author Michael Burman
 */
public class EventCondition extends AbstractCondition {

    private String expression;
    private String srcExpression;

    public EventCondition severity(EventSeverity severity) {
        setName(severity.toString());
        return this;
    }

    public EventCondition expression(String regexp) {
        this.expression = regexp;
        return this;
    }

    public EventCondition location(String regexp) {
        this.srcExpression = regexp;
        return this;
    }

    @Override
    AlertConditionCategory getCategory() {
        return AlertConditionCategory.EVENT;
    }

    @Override
    public AlertCondition getAlertCondition() {
        AlertCondition eventCondition = super.getAlertCondition();
        StringBuilder sb = new StringBuilder();
        sb.append(expression);
        sb.append("@@@");
        sb.append(srcExpression);
        eventCondition.setOption(sb.toString());
        return eventCondition;
    }
}
