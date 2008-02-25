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
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * all methods that aren't getters appropriately update the contents of the AlertConditionCache
 *
 * @author Greg Hinkle
 */
@Local
public interface AlertDefinitionManagerLocal {
    PageList<AlertDefinition> getAllAlertDefinitionsWithConditions();

    PageList<AlertDefinition> getAlertDefinitions(Subject user, int resourceId, PageControl pageControl);

    AlertDefinition getAlertDefinitionById(Subject user, int alertDefinitionId);

    int createAlertDefinition(Subject user, AlertDefinition alertDefinition, Integer resourceId)
        throws InvalidAlertDefinitionException;

    int removeAlertDefinitions(Subject user, Integer[] alertDefinitionIds);

    int enableAlertDefinitions(Subject user, Integer[] alertDefinitionIds);

    int disableAlertDefinitions(Subject user, Integer[] alertDefinitionIds);

    void copyAlertDefinitions(Subject user, Integer[] alertDefinitionIds);

    AlertDefinition updateAlertDefinition(Subject user, AlertDefinition alertDefinition)
        throws InvalidAlertDefinitionException, AlertDefinitionUpdateException;
}