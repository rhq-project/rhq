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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertDampeningEvent;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.jms.model.AbstractAlertConditionMessage;
import org.rhq.enterprise.server.alert.engine.jms.model.ActiveAlertConditionMessage;
import org.rhq.enterprise.server.alert.engine.jms.model.InactiveAlertConditionMessage;

/**
 * see {@link CachedConditionManagerLocal#processCachedConditionMessage(AbstractAlertConditionMessage, AlertDefinition)}
 * for more information.
 * 
 * @author Joseph Marques
 */
@Stateless
public class CachedConditionManagerBean implements CachedConditionManagerLocal {

    private final Log log = LogFactory.getLog(CachedConditionManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AlertConditionLogManagerLocal alertConditionLogManager;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void processCachedConditionMessage(AbstractAlertConditionMessage conditionMessage, Integer definitionId) {
        /*
         * note that ctime is the time when the condition was known to be true, not the time we're persisting the
         * condition log message
         */
        if (conditionMessage instanceof ActiveAlertConditionMessage) {
            ActiveAlertConditionMessage activeConditionMessage = (ActiveAlertConditionMessage) conditionMessage;

            alertConditionLogManager.updateUnmatchedLogByAlertConditionId(activeConditionMessage.getAlertConditionId(),
                activeConditionMessage.getTimestamp(), activeConditionMessage.getValue());

            alertConditionLogManager.checkForCompletedAlertConditionSet(activeConditionMessage.getAlertConditionId());
        } else if (conditionMessage instanceof InactiveAlertConditionMessage) {
            // first do some bookkeeping by removing partially matched condition logs 
            alertConditionLogManager.removeUnmatchedLogByAlertConditionId(conditionMessage.getAlertConditionId());

            // then create a NEGATIVE dampening event, to breakup any contiguous POSITIVE events for correct processing
            AlertDefinition flyWeightDefinition = new AlertDefinition();
            flyWeightDefinition.setId(definitionId);
            AlertDampeningEvent event = new AlertDampeningEvent(flyWeightDefinition, AlertDampeningEvent.Type.NEGATIVE);
            entityManager.persist(event);
        } else {
            log.error("Unsupported message type sent to consumer for processing: "
                + conditionMessage.getClass().getSimpleName());
        }
    }
}
