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

import java.util.ArrayList;
import java.util.List;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;

/**
 * Describes <code>EVENT </code> {@link AlertCondition}s.
 *
 * @author Justin Harris
 */
public class EventDescriber extends AlertConditionDescriber {

    @Override
    public AlertConditionCategory[] getDescribedCategories() {
        return makeCategories(AlertConditionCategory.EVENT);
    }

    // TODO:  This is a little crazy - refactor?
    @Override
    public void createDescription(AlertCondition condition, StringBuilder builder) {
        String msgKey = "alert.config.props.CB.EventSeverity";
        List<String> args = new ArrayList<String>(2);

        args.add(condition.getName());
        if ((condition.getOption() != null) && (condition.getOption().length() > 0)) {
            msgKey += ".RegexMatch";
            args.add(condition.getOption());
        }

        builder.append(translate(msgKey, args.toArray()));
    }
}
