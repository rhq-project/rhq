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

import java.text.MessageFormat;
import java.util.Map;
import org.rhq.core.domain.alert.AlertDampening;

public class AlertDampeningDescriber {

    private final static String TIME_UNIT_PREFIX = "alert.config.props.CB.Enable.TimeUnit.";
    private final static String DAMPEN_PREFIX = "alert.config.props.CB.Dampen";

    private Map<String, String> translations;

    public AlertDampeningDescriber(Map<String, String> translations) {
        this.translations = translations;
    }

    public String describe(AlertDampening dampening) {
        int howLong = dampening.getPeriod();
        String howLongUnits = translate(TIME_UNIT_PREFIX + dampening.getPeriodUnits());

        int howMany = dampening.getValue();
        String howManyUnits = translate(TIME_UNIT_PREFIX + dampening.getValueUnits());
        String enableActionResource = DAMPEN_PREFIX + toCamelCase(dampening.getCategory());

        return translate(enableActionResource, howLong, howLongUnits, howMany, howManyUnits);
    }

    // NOTE:  This is copied directly from AlertConditionDescriber -- refactor!
    private String translate(final String key, final Object... params) {
        String message = this.translations.get(key);

        if (params.length == 0) {
            return message;
        } else {
            return MessageFormat.format(message, params);
        }
    }

    // Converts e.g. AlertDampening.Category.PARTIAL_COUNT to "PartialCount"
    private String toCamelCase(AlertDampening.Category category) {
        StringBuilder categoryName = new StringBuilder(category.name().toLowerCase());

        String first = "" + categoryName.charAt(0);
        categoryName.replace(0, 1, first.toUpperCase());

        int index = categoryName.indexOf("_");
        while (index > -1) {
            String after = "" + categoryName.charAt(index + 1);
            categoryName.replace(index, index + 2, after.toUpperCase());

            index = categoryName.indexOf("_");
        }

        return categoryName.toString();
    }
}