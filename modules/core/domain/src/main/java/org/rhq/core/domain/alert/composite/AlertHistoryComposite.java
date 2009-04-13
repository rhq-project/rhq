/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.alert.composite;

import org.rhq.core.domain.alert.Alert;

/**
 * @author Joseph Marques
 */
public class AlertHistoryComposite {

    private final Alert alert;
    private final Integer parentResourceId;
    private final String parentResourceName;

    private String conditionText;
    private String conditionValue;
    private String recoveryInfo;

    public AlertHistoryComposite(Alert alert, Integer parentResourceId, String parentResourceName) {
        this.alert = alert;
        this.parentResourceId = parentResourceId;
        this.parentResourceName = parentResourceName;
    }

    public Alert getAlert() {
        return alert;
    }

    public Integer getParentResourceId() {
        return parentResourceId;
    }

    public String getParentResourceName() {
        return parentResourceName;
    }

    public String getConditionText() {
        return conditionText;
    }

    public void setConditionText(String conditionText) {
        this.conditionText = conditionText;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(String conditionValue) {
        this.conditionValue = conditionValue;
    }

    public String getRecoveryInfo() {
        return recoveryInfo;
    }

    public void setRecoveryInfo(String recoveryInfo) {
        this.recoveryInfo = recoveryInfo;
    }

}
