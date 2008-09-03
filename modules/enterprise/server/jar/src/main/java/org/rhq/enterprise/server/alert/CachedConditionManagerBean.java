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

@Stateless
public class CachedConditionManagerBean implements CachedConditionManagerLocal {

    private final Log log = LogFactory.getLog(CachedConditionManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AlertConditionLogManagerLocal alertConditionLogManager;
    @EJB
    private AlertDampeningManagerLocal alertDampeningManager;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void processCachedConditionMessage(AbstractAlertConditionMessage conditionMessage, AlertDefinition definition) {
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

            AlertDampeningEvent event = new AlertDampeningEvent(definition, AlertDampeningEvent.Type.NEGATIVE);
            entityManager.persist(event);

            alertDampeningManager.processEventType(definition.getId(), AlertDampeningEvent.Type.NEGATIVE);
        } else {
            log.error("Unsupported message type sent to consumer for processing: "
                + conditionMessage.getClass().getSimpleName());
        }
    }
}
