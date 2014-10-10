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
import org.rhq.core.domain.alert.builder.AlertDefinitionTemplate;
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
     * Get the alert definition with the given id. It may be a template, group or resource level alert definition.
     * <p/>
     * Requires VIEW permission for the group or resource on which the alert definition is defined. Templates are
     * viewable by any user.
     *
     * @param subject
     * @param alertDefinitionId
     * @return the alert definition or null
     */
    AlertDefinition getAlertDefinition(Subject subject, int alertDefinitionId);

    /**
     * Get the alert definitions specified by the given criteria. The returned list may contain any combination
     * of template, group or resource level alert definitions.
     * definition.
     * <p/>
     * Requires VIEW permission for the group or resource on which the alert definition is defined. Templates are
     * viewable by any user.
     *
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<AlertDefinition> findAlertDefinitionsByCriteria(Subject subject, AlertDefinitionCriteria criteria);

    /**
     * Enable the specified alert definitions. Currently enabled alert definitions are ignored, as are definitions for
     * which the proper permission is not held.
     * <p/>
     * Requires MANAGE_ALERTS permission for the group or resource on which the alert definition is defined. REQUIRES
     * MANAGE_SETTINGS for Templates.
     *
     * @param subject
     * @param alertDefinitionIds
     * @return number of modified definitions
     */
    int enableAlertDefinitions(Subject subject, int[] alertDefinitionIds);

    /**
     * Disable the specified alert definitions. Currently disabled alert definitions are ignored, as are definitions for
     * which the proper permission is not held.
     * <p/>
     * Requires MANAGE_ALERTS permission for the group or resource on which the alert definition is defined. REQUIRES
     * MANAGE_SETTINGS for Templates.
     *
     * @param subject
     * @param alertDefinitionIds
     * @return number of modified definitions
     */
    int disableAlertDefinitions(Subject subject, int[] alertDefinitionIds);

    /**
     * Remove the specified alert definitions. Currently deleted alert definitions are ignored, as are definitions for
     * which the proper permission is not held.
     * <p/>
     * Requires MANAGE_ALERTS permission for the group or resource on which the alert definition is defined. REQUIRES
     * MANAGE_SETTINGS for Templates.
     *
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

    /**
     * Creates a new alert definition. Note that the subject is checked to have necessary rights.
     * <p />
     * Requires MANAGE_ALERTS permission for the group or resource on which the alert definition is defined.
     *
     * @param subject the user creating the alert definition
     * @param template definition template which is used to create new AlertDefinition
     * @return The AlertDefinition that was just created
     */
    AlertDefinition createAlertDefinitionFromTemplate(Subject subject, AlertDefinitionTemplate template);
}
