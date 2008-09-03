package org.rhq.enterprise.server.alert;

import javax.ejb.Local;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.enterprise.server.alert.engine.jms.model.AbstractAlertConditionMessage;

@Local
public interface CachedConditionManagerLocal {
    void processCachedConditionMessage(AbstractAlertConditionMessage conditionMessage, AlertDefinition definition);
}
