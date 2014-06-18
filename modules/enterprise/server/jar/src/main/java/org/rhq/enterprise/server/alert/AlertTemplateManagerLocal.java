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

import javax.ejb.Local;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;

/**
 * @author Joseph Marques
 */

@Local
public interface AlertTemplateManagerLocal {
    PageList<AlertDefinition> getAlertTemplates(Subject user, int resourceTypeId, PageControl pageControl);

    int createAlertTemplate(Subject user, AlertDefinition alertDefinition, Integer resourceTypeId)
        throws InvalidAlertDefinitionException, ResourceTypeNotFoundException, AlertDefinitionCreationException;

    // this is a system side-effect of template processing, and thus should only ever by called by the overlord user
    void updateAlertDefinitionsForResource(Subject user, Integer resourceId) throws AlertDefinitionCreationException,
        InvalidAlertDefinitionException;

    /**
     * This can update a lot of resource-level alert definitions.  It is recommended to operate on enable a single
     * alert template in one call.
     * <p>
     * Requires MANAGE_SETTINGS
     *
     * @param user
     * @param alertTemplateIds
     */
    void removeAlertTemplates(Subject user, Integer[] alertTemplateIds);

    /**
     * This can update a lot of resource-level alert definitions.  It is recommended to operate on a single
     * alert template in one call.
     * <p>
     * Requires MANAGE_SETTINGS
     *
     * @param user
     * @param alertTemplateIds
     */
    void enableAlertTemplates(Subject user, Integer[] alertTemplateIds);

    /**
     * This can update a lot of resource-level alert definitions.  It is recommended to operate on enable a single
     * alert template in one call.
     * <p>
     * Requires MANAGE_SETTINGS
     *
     * @param user
     * @param alertTemplateIds
     */
    void disableAlertTemplates(Subject user, Integer[] alertTemplateIds);

    AlertDefinition updateAlertTemplate(Subject user, AlertDefinition alertDefinition, boolean purgeInternals)
        throws InvalidAlertDefinitionException, AlertDefinitionUpdateException;
}