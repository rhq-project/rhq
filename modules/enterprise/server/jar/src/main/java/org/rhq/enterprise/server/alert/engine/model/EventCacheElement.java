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

import java.util.regex.Pattern;

import org.rhq.core.domain.event.EventSeverity;

/**
 * @author Joseph Marques
 */

public class EventCacheElement extends AbstractEnumCacheElement<EventSeverity> {

    private final Pattern eventDetailsPattern;

    private String fixPattern(String regex) {
        boolean sw = regex.startsWith(".*");
        boolean ew = regex.endsWith(".*");
        return (!sw ? ".*" : "") + regex + (!ew ? ".*" : "");
    }

    public EventCacheElement(AlertConditionOperator operator, EventSeverity value, int conditionTriggerId) {
        super(operator, value, conditionTriggerId);
        eventDetailsPattern = null;
    }

    public EventCacheElement(AlertConditionOperator operator, String eventDetails, EventSeverity value,
        int conditionTriggerId) {
        super(operator, eventDetails, value, conditionTriggerId);
        eventDetails = fixPattern(eventDetails);
        eventDetailsPattern = Pattern.compile(eventDetails, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
            | Pattern.DOTALL);
    }

    @Override
    public boolean matches(EventSeverity providedValue, Object... extraParams) {
        boolean matches = super.matches(providedValue, extraParams);

        if (matches && eventDetailsPattern != null) {
            Object firstParam = extraParams[0];
            if (!(firstParam instanceof String)) {
                log.error(getClass().getSimpleName() + " expected a String, but recieved a "
                    + extraParams.getClass().getSimpleName());
            } else {
                matches = matches && eventDetailsPattern.matcher((String) firstParam).matches();
            }
        }

        return matches;
    }

    @Override
    public AlertConditionOperator.Type getOperatorSupportsType(AlertConditionOperator operator) {
        if ((operator == AlertConditionOperator.CHANGES_TO) || (operator == AlertConditionOperator.CHANGES_FROM)) {
            return AlertConditionOperator.Type.NONE;
        }

        return super.getOperatorSupportsType(operator);
    }

}
