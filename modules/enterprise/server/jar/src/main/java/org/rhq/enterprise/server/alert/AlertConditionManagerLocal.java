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

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.composite.AbstractAlertConditionCategoryComposite;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Joseph Marques
 */

@Local
public interface AlertConditionManagerLocal {

    Integer getAlertDefinitionByConditionIdNewTx(int alertConditionId);

    AlertCondition getAlertConditionById(int alertConditionId);

    PageList<? extends AbstractAlertConditionCategoryComposite> getAlertConditionComposites(Subject user,
        Integer agentId, AlertConditionCategory category, PageControl pageControl);

    InventoryStatus getResourceStatusByConditionIdNewTx(int alertConditionId);

    int purgeOrphanedAlertConditions();
}