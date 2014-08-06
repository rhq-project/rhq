/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.integrationTests.restApi.d;

import java.util.HashMap;
import java.util.Map;

/**
 * An alert notification for testing purposes
 * @author Heiko W. Rupp
 */
public class AlertNotification {

    private int id;
    private String senderName;
    private Map<String,Object> config = new HashMap<String, Object>();
    private Map<String, Object> extraConfig = new HashMap<String, Object>();

    public AlertNotification() {
    }

    public AlertNotification(String senderName) {
        this.senderName = senderName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public void setExtraConfig(Map<String, Object> extraConfig) {
        this.extraConfig = extraConfig;
    }

    public Map<String, Object> getExtraConfig() {
        return extraConfig;
    }
}
