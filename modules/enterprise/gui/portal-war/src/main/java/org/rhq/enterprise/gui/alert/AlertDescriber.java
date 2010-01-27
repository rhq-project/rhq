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

import java.util.HashMap;
import java.util.Map;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.enterprise.gui.alert.description.AlertConditionDescriber;
import org.rhq.enterprise.gui.alert.description.AlertDampeningDescriber;
import org.rhq.enterprise.gui.alert.description.AvailabilityDescriber;
import org.rhq.enterprise.gui.alert.description.BaselineDescriber;
import org.rhq.enterprise.gui.alert.description.ChangeOrTraitDescriber;
import org.rhq.enterprise.gui.alert.description.ControlDescriber;
import org.rhq.enterprise.gui.alert.description.DefaultDescriber;
import org.rhq.enterprise.gui.alert.description.EventDescriber;
import org.rhq.enterprise.gui.alert.description.ResourceConfigDescriber;
import org.rhq.enterprise.gui.alert.description.ThresholdDescriber;

/**
 * Facade component that gives a natural language description of various alert-related objects.
 *
 * @author Justin Harris
 */
@Name("alertDescriber")
@Scope(ScopeType.APPLICATION)
@AutoCreate
public class AlertDescriber {

    @In
    private Map<String, String> messages;
    private Map<AlertConditionCategory, AlertConditionDescriber> conditionDescribers;
    private AlertDampeningDescriber dampeningDescriber;

    @Create
    public void init() {
        this.dampeningDescriber = new AlertDampeningDescriber(this.messages);
        this.conditionDescribers = new HashMap<AlertConditionCategory, AlertConditionDescriber>();

        // TODO:  Assemble this list dynamically?
        AlertConditionDescriber[] describerList = new AlertConditionDescriber[] {
            new ControlDescriber(),
            new ResourceConfigDescriber(),
            new ThresholdDescriber(),
            new BaselineDescriber(),
            new ChangeOrTraitDescriber(),
            new EventDescriber(),
            new AvailabilityDescriber(),
            new DefaultDescriber()
        };

        for (AlertConditionDescriber describer : describerList) {
            describer.setTranslations(this.messages);

            for (AlertConditionCategory category : describer.getDescribedCategories()) {
                this.conditionDescribers.put(category, describer);
            }
        }
    }

    /**
     * Describes the given {@link AlertCondition} using the Seam locale.
     *
     * @param condition
     * @return a natural language description of the <code>condition</code>
     */
    public String describeCondition(AlertCondition condition) {
        AlertConditionDescriber describer = this.conditionDescribers.get(condition.getCategory());

        if (describer != null) {
            return describer.describe(condition);
        }

        return null;
    }

    /**
     * Describes the given {@link AlertDampening} using the Seam locale.
     *
     * @param dampening
     * @return
     */
    public String describeDampening(AlertDampening dampening) {
        return this.dampeningDescriber.describe(dampening);
    }
}