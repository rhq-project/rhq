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

import org.jboss.annotation.ejb.PoolClass;
import org.jboss.ejb3.StrictMaxPool;
import org.rhq.core.domain.alert.AlertDampeningEvent;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.jms.model.AbstractAlertConditionMessage;
import org.rhq.enterprise.server.alert.engine.jms.model.ActiveAlertConditionMessage;
import org.rhq.enterprise.server.alert.engine.jms.model.InactiveAlertConditionMessage;

/**
 * see {@link CachedConditionManagerLocal#processCachedConditionMessage(
 * org.rhq.enterprise.server.alert.engine.jms.model.AbstractAlertConditionMessage, Integer)}
 * for more information.
 * 
 * @author Joseph Marques
 */
@Stateless
// NOTE: The AlertConditionLogManagerBean, AlertConditionManagerBean, AlertDampeningManagerBean,
//       AlertDefinitionManagerBean, and CachedConditionManagerBean SLSB's are all invoked, either directly or
//       indirectly, by the AlertConditionConsumerBean MDB. Since MDB invocations are always done in new threads, using
//       the default SLSB pool impl ({@link ThreadlocalPool}) would cause a new instance of this SLSB to be created
//       every time it was invoked by AlertConditionConsumerBean. This would be bad because an existing instance would
//       not be reused, but it is really bad because the instance would also never get destroyed, causing heap space to
//       gradually leak until the Server eventually ran out of memory. Hence, we must use a {@link StrictMaxPool}, which
//       will use a fixed pool of instances of this SLSB, instead of a ThreadlocalPool. Because most of these SLSB's are
//       also invoked by other callers (i.e. Agents, GUI's, or CLI's) besides AlertConditionConsumerBean, we set the max
//       pool size to 60, which is double the default value, to minimize the chances of AlertConditionConsumerBean
//       invocations, which are the most critical, from having to block and potentially getting backed up in the queue.
//       For more details, see https://bugzilla.redhat.com/show_bug.cgi?id=693232 (ips, 05/05/11).
@PoolClass(value = StrictMaxPool.class, maxSize = 60)
public class CachedConditionManagerBean implements CachedConditionManagerLocal {

    private final Log log = LogFactory.getLog(CachedConditionManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;

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

            if (alertDefinitionManager.isEnabled(definitionId) == false) {
                if (log.isDebugEnabled()) {
                    log.debug("AlertDefinition[id=" //
                        + activeConditionMessage.getAlertConditionId() //
                        + "] was already disabled " //
                        + "(likely due to recovery logic disablement on earlier messages in this process batch), " //
                        + "ignoring " //
                        + activeConditionMessage);
                }
                return;
            }

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
