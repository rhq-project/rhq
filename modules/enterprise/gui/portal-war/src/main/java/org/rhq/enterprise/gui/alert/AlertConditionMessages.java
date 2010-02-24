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
package org.rhq.enterprise.gui.alert;

import java.text.MessageFormat;
import java.util.Map;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Scope(ScopeType.APPLICATION)
@Name("alertConditionMessages")
public class AlertConditionMessages {

    @In
    private Map<String, String> messages;

    public String getThreshold() {
        return translate("errors.double", "Threshold");
    }

    public String getPercentRange() {
        return translate("errors.range", "Threshold", "0%", "1000%");
    }

    public String getDampeningCount() {
        return translate("errors.integer", "Dampening Count");
    }

    public String getDampeningEvaluation() {
        return translate("errors.integer", "Dampening Evaluations");
    }

    public String getTimePeriod() {
        return translate("errors.integer", "Time Period");
    }

    private String translate(String key, Object... params) {
        String message = messages.get(key);

        return MessageFormat.format(message, params);
    }
}