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

    public AbstractNotificationSenderForm(String locatorId) {
        super(locatorId);
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
}
