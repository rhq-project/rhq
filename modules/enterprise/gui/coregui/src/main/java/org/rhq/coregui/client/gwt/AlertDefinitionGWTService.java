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
package org.rhq.coregui.client.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.util.PageList;

public interface AlertDefinitionGWTService extends RemoteService {

    PageList<AlertDefinition> findAlertDefinitionsByCriteria(AlertDefinitionCriteria criteria) throws RuntimeException;

    AlertDefinition createAlertDefinition(AlertDefinition alertDefinition, Integer resourceId) throws RuntimeException;

    AlertDefinition updateAlertDefinition(int alertDefinitionId, AlertDefinition alertDefinition, boolean purgeInternals)
        throws RuntimeException;

    int enableAlertDefinitions(int[] alertDefinitionIds) throws RuntimeException;

    int disableAlertDefinitions(int[] alertDefinitionIds) throws RuntimeException;

    int removeAlertDefinitions(int[] alertDefinitionIds) throws RuntimeException;

    int removeAlertDefinitions(Integer groupId, int[] alertDefinitionIds) throws RuntimeException;

    String[] getAlertNotificationConfigurationPreview(AlertNotification[] notifs) throws RuntimeException;

    String[] getAllAlertSenders() throws RuntimeException;

    ConfigurationDefinition getConfigurationDefinitionForSender(String sender) throws RuntimeException;
}