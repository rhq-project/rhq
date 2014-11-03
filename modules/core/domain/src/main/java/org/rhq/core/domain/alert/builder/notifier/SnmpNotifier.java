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

package org.rhq.core.domain.alert.builder.notifier;

import org.rhq.core.domain.alert.builder.AlertNotificationTemplate;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Michael Burman
 */
public class SnmpNotifier extends AlertNotificationTemplate {

    private String host = null;
    private Integer port = null;
    private String prefixOid = null;
    private String trapOid = null;

    public SnmpNotifier host(String host) {
        this.host = host;
        return this;
    }

    public SnmpNotifier port(Integer port) {
        this.port = port;
        return this;
    }

    public SnmpNotifier variablePrefix(String prefix) {
        this.prefixOid = prefix;
        return this;
    }

    public SnmpNotifier trap(String trap) {
        this.trapOid = trap;
        return this;
    }

    @Override
    public AlertNotification getAlertNotification() {
        sender("SnmpSender");
        Configuration configuration = new Configuration();
        if(prefixOid != null) {
            configuration.setSimpleValue("oid", prefixOid);
        } else {
            throw new IllegalArgumentException("Variable binding prefix is mandatory");
        }

        if(host != null) {
            configuration.setSimpleValue("host", host);
        }
        if(port != null) {
            PropertySimple simple = new PropertySimple("port", port);
            configuration.put(simple);
        }
        if(trapOid != null) {
            configuration.setSimpleValue("trapOid", trapOid);
        }

        configuration(configuration);

        return super.getAlertNotification();
    }
}
