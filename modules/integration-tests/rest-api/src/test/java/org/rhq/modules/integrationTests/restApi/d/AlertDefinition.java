/*
 * RHQ Management Platform
 *  Copyright (C) 2005-2013 Red Hat, Inc.
 *  All rights reserved.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation version 2 of the License.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.integrationTests.restApi.d;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * An AlertDefinition for testing purposes
 * @author Heiko W. Rupp
 */
@XmlRootElement(name = "definition")
public class AlertDefinition {

    int id;
    String name;
    boolean enabled;
    String priority;
    int recoveryId;
    String conditionMode = "ANY";
    List<AlertCondition> conditions = new ArrayList<AlertCondition>();
    List<AlertNotification> notifications = new ArrayList<AlertNotification>();
    String dampeningCategory = "NONE";
    String dampeningCount;
    String dampeningPeriod;


    public AlertDefinition() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public int getRecoveryId() {
        return recoveryId;
    }

    public void setRecoveryId(int recoveryId) {
        this.recoveryId = recoveryId;
    }

    public String getConditionMode() {
        return conditionMode;
    }

    public void setConditionMode(String conditionMode) {
        this.conditionMode = conditionMode;
    }

    public List<AlertCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<AlertCondition> conditions) {
        this.conditions = conditions;
    }

    public List<AlertNotification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<AlertNotification> notifications) {
        this.notifications = notifications;
    }

    public String getDampeningCategory() {
        return dampeningCategory;
    }

    public void setDampeningCategory(String dampeningCategory) {
        this.dampeningCategory = dampeningCategory;
    }

    public String getDampeningCount() {
        return dampeningCount;
    }

    public void setDampeningCount(String dampeningCount) {
        this.dampeningCount = dampeningCount;
    }

    public String getDampeningPeriod() {
        return dampeningPeriod;
    }

    public void setDampeningPeriod(String dampeningPeriod) {
        this.dampeningPeriod = dampeningPeriod;
    }
}
