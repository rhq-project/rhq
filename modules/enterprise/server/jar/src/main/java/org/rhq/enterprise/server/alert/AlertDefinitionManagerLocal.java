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

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.composite.IntegerOptionItem;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.FetchException;

/**
 * all methods that aren't getters appropriately update the contents of the AlertConditionCache
 *
 * @author Joseph Marques
 */
@Local
public interface AlertDefinitionManagerLocal {
    List<AlertDefinition> findAllAlertDefinitionsWithConditions(int agentId, Subject user);

    PageList<AlertDefinition> findAlertDefinitions(Subject subject, int resourceId, PageControl pageControl);

    AlertDefinition getAlertDefinitionById(Subject subject, int alertDefinitionId);

    List<IntegerOptionItem> findAlertDefinitionOptionItems(Subject subject, int resourceId);

    int createAlertDefinition(Subject subject, AlertDefinition alertDefinition, Integer resourceId)
        throws InvalidAlertDefinitionException;

    int removeAlertDefinitions(Subject subject, Integer[] alertDefinitionIds);

    int enableAlertDefinitions(Subject subject, Integer[] alertDefinitionIds);

    boolean isEnabled(Integer definitionId);

    boolean isTemplate(Integer definitionId);

    int disableAlertDefinitions(Subject subject, Integer[] alertDefinitionIds);

    List<AlertDefinition> findAllRecoveryDefinitionsById(Subject subject, Integer alertDefinitionId);

    void copyAlertDefinitions(Subject subject, Integer[] alertDefinitionIds);

    AlertDefinition updateAlertDefinition(Subject subject, int alertDefinitionId, AlertDefinition alertDefinition,
        boolean updateInternals) throws InvalidAlertDefinitionException, AlertDefinitionUpdateException;

    int purgeUnusedAlertDefinition();

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    AlertDefinition getAlertDefinition(Subject subject, int alertDefinitionId) throws FetchException;

    PageList<AlertDefinition> findAlertDefinitions(Subject subject, AlertDefinition criteria, PageControl pc)
        throws FetchException;
}