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

import org.rhq.core.domain.alert.AlertConditionCategory;

/**
 * @author Michael Burman
 */
public class DriftCondition extends AbstractCondition {

    public DriftCondition name(String name) {
        setName(name);
        return this;
    }

    public DriftCondition expression(String regexp) {
        overrideOption(regexp);
        return this;
    }

    @Override
    AlertConditionCategory getCategory() {
        return AlertConditionCategory.DRIFT;
    }
}
