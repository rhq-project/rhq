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

package org.rhq.core.domain.alert.builder;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.BooleanExpression;
import org.rhq.core.domain.alert.builder.condition.AbstractCondition;

/**
 * This builder can be used to construct AlertDefinitions.
 *
 * @author Michael Burman
 */
public class AlertDefinitionTemplate implements Serializable {
    private Set<AbstractCondition> conditions;
    private Set<AlertNotificationTemplate> notifiers;

    private AlertDampeningTemplate dampening = null;

    private boolean enabled = true;
    private String description = null;
    private String name = null;
    private AlertPriority priority = null;
    private BooleanExpression protocol = null;
    private Recovery recovery = null;
    private Integer resourceId;

    public AlertDefinitionTemplate(Integer resourceId, String name) {
        conditions = new HashSet<AbstractCondition>();
        notifiers = new HashSet<AlertNotificationTemplate>();
        dampening = new AlertDampeningTemplate().category(AlertDampening.Category.NONE);
        this.recovery = new Recovery();
        this.resourceId = resourceId;
        priority = AlertPriority.MEDIUM;
        protocol = BooleanExpression.ANY;
        name(name);
    }

    public AlertDefinitionTemplate enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public AlertDefinitionTemplate description(String description) {
        this.description = description;
        return this;
    }

    public AlertDefinitionTemplate name(String name) {
        this.name = name;
        return this;
    }

    public AlertDefinitionTemplate priority(AlertPriority priority) {
        this.priority = priority;
        return this;
    }

    public AlertDefinitionTemplate alertProtocol(BooleanExpression protocol) {
        this.protocol = protocol;
        return this;
    }

    public static class Recovery implements Serializable {
        private Boolean disableWhenFired = false;
        private Integer recoveredAlert = Integer.valueOf(0); // 0 indicates nothing to recover

        Boolean getDisableWhenFired() {
            return disableWhenFired;
        }

        Integer getRecoveredAlert() {
            return recoveredAlert;
        }

        public Recovery disableWhenFired(Boolean disable) {
            this.disableWhenFired = disable;
            return this;
        }

        public Recovery recoverAlert(Integer alertToRecover) {
            recoveredAlert = alertToRecover;
            return this;
        }
    }

    public Recovery recovery() {
        return this.recovery;
    }

    // Redesign this part..
    public AlertDefinitionTemplate dampening(AlertDampeningTemplate dampening) {
        this.dampening = dampening;
        return this;
    }

    public AlertDefinitionTemplate addCondition(AbstractCondition condition) {
        this.conditions.add(condition);
        return this;
    }

    public AlertDefinitionTemplate addNotification(AlertNotificationTemplate notifier) {
        notifiers.add(notifier);
        return this;
    }

    /**
     * Transforms this template to an AlertDefinition based on the info that has been entered. Does not check for validity
     * of the created instance.
     *
     * @return AlertDefinition with values
     */
    public AlertDefinition getAlertDefinition() {
        AlertDefinition alertDefinition = new AlertDefinition();

        // General properties
        alertDefinition.setEnabled(this.enabled);
        alertDefinition.setDescription(this.description);
        alertDefinition.setName(this.name);
        alertDefinition.setConditionExpression(this.protocol);
        alertDefinition.setPriority(this.priority);

        // Recovery properties
        if(this.recovery != null) {
            alertDefinition.setRecoveryId(this.recovery.getRecoveredAlert());
            alertDefinition.setWillRecover(this.recovery().getDisableWhenFired());
        }

        // Dampening properties
        if(this.dampening != null) {
            alertDefinition.setAlertDampening(this.dampening.getAlertDampening());
        }

        // Notification properties
        for (AlertNotificationTemplate notifier : notifiers) {
            alertDefinition.addAlertNotification(notifier.getAlertNotification());
        }

        // Conditions - not returned here, use AlertDefinitionManagerBean to get enriched conditions

        return alertDefinition;
    }

    /**
     * Get AlertConditionTemplates. They can't be transformed here to AlertConditions
     * @return
     */
    public Set<AbstractCondition> getConditions() {
        return conditions;
    }

    public Integer getResourceId() {
        return resourceId;
    }
}
