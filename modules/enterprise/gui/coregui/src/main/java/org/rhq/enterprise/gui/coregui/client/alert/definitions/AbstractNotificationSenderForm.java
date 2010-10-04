/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Superclass to all alert senders' forms. Instances of this class
 * are displayed when switching the drop-down option menu selecting
 * a specific sender.
 *  
 * @author John Mazzitelli
 */
public abstract class AbstractNotificationSenderForm extends LocatableVLayout {

    private Configuration configuration;
    private Configuration extraConfiguration;
    private final String sender;

    public AbstractNotificationSenderForm(String locatorId, AlertNotification notif, String sender) {
        super(locatorId);

        this.sender = sender;

        if (notif != null) {
            // make our own deep copies of the configs so we can throw them away without
            // affecting the actual notif in case the user clicks cancel
            Configuration notifConfig = notif.getConfiguration();
            Configuration notifExtraConfig = notif.getExtraConfiguration();

            if (notifConfig != null) {
                configuration = notifConfig.deepCopy(true);
            } else {
                configuration = new Configuration(); // must not be null
            }
            if (notifExtraConfig != null) {
                extraConfiguration = notifExtraConfig.deepCopy(true);
            } else {
                extraConfiguration = null; // allowed to be null
            }
        } else {
            configuration = new Configuration();
            extraConfiguration = null;
        }
    }

    /**
     * The name of the alert sender that is to be configured.
     * @return the sender name
     */
    public String getSender() {
        return sender;
    }

    /**
     * The {@link AlertNotification#getConfiguration()} for the new alert definition notification.
     * 
     * @return the configuration
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * The {@link AlertNotification#getExtraConfiguration()} for the new alert definition notification.
     * 
     * @return the extra configuration
     */
    public Configuration getExtraConfiguration() {
        return extraConfiguration;
    }

    public void setExtraConfiguration(Configuration extraConfiguration) {
        this.extraConfiguration = extraConfiguration;
    }

    public abstract boolean validate();
}
