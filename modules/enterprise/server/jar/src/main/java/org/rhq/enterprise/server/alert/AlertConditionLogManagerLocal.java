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

import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.BooleanExpression;

/**
 * @author Joseph Marques
 */

@Local
public interface AlertConditionLogManagerLocal {
    AlertConditionLog getUnmatchedLogByAlertConditionId(int alertConditionId);

    List<AlertConditionLog> getUnmatchedLogsByAlertDefinitionId(int alertDefinitionId);

    void updateUnmatchedLogByAlertConditionId(int alertConditionId, long ctime, String value);

    void removeUnmatchedLogByAlertConditionId(int alertConditionId);

    void checkForCompletedAlertConditionSet(int alertConditionId);

    /**
     * Used for internal processing, exposed here so the tiniest amount of data can be gotten in a new transaction
     * 
     * @param alertDefinitionId the definition whose conditionExpression will be looked up
     * @return the BooleanException for the AlertDefinition represented by the id argument
     */
    BooleanExpression getConditionExpression(int alertDefinitionId);

    /**
     * Used for internal processing, exposed here so the tiniest amount of data can be gotten in a new transaction
     * 
     * @param alertDefinitionId the definition whose conditionExpression will be looked up
     * @return the count of AlertConditions for the AlertDefinition represented by the id argument
     */
    int getConditionCount(int alertDefinitionId);
}