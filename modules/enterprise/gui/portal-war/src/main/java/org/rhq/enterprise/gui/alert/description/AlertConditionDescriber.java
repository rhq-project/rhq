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
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;

/**
 * Knows how to describe an {@link AlertCondition} in natural language, but only
 * if the condition is only in certain {@link AlertConditionCategory} list.
 *
 * @author Justin Harris
 */
public abstract class AlertConditionDescriber {

    private Map<String, String> translations;

    /**
     * Return the categories that this describer knows how to describe.
     *
     * @return
     */
    public abstract AlertConditionCategory[] getDescribedCategories();

    /**
     * Actually builds the description of the given <code>condition</code>, modifying
     * the given string builder.
     *
     * @param condition
     * @param builder
     */
    public abstract void createDescription(AlertCondition condition, StringBuilder builder);

    public void setTranslations(Map<String, String> translations) {
        this.translations = translations;
    }

    /**
     * Describes the given {@link AlertCondition} using the Seam locale.
     *
     * @param condition
     * @return a natural language description of the <code>condition</code>
     */
    public final String describe(AlertCondition condition) {
        StringBuilder builder = new StringBuilder();

        createDescription(condition, builder);

        return builder.toString();
    }

    protected String translate(final String key, final Object... params) {
        String message = this.translations.get(key);

        if (params.length == 0) {
            return message;
        } else {
            return MessageFormat.format(message, params);
        }
    }

    // Kind of a weird hack to make array creation nicer on the eyes
    protected AlertConditionCategory[] makeCategories(AlertConditionCategory... categories) {
        return categories;
    }
}
