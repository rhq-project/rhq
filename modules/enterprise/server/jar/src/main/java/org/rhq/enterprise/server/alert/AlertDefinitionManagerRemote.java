/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.alert;

import javax.ejb.Remote;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.util.PageList;

/**
 * Public API for Alert Definitions.
 */
@Remote
public interface AlertDefinitionManagerRemote {

    /**
     * @param subject
     * @param alertDefinitionId
     * @return the alert definition or null
     */
    AlertDefinition getAlertDefinition(Subject subject, int alertDefinitionId);

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<AlertDefinition> findAlertDefinitionsByCriteria(Subject subject, AlertDefinitionCriteria criteria);

    /**
     * @param subject
     * @param alertDefinitionIds
     * @return number of modified definitions
     */
    int enableAlertDefinitions(Subject subject, int[] alertDefinitionIds);

    /**
     * @param subject
     * @param alertDefinitionIds
     * @return number of modified definitions
     */
    int disableAlertDefinitions(Subject subject, int[] alertDefinitionIds);

    /**
     * @param subject
     * @param alertDefinitionIds
     * @return number of removed definitions
     */
    int removeAlertDefinitions(Subject subject, int[] alertDefinitionIds);

    /**
     * @param subject
     * @param notifications
     * @return not null
     */
    String[] getAlertNotificationConfigurationPreview(Subject subject, AlertNotification[] notifications);

}
