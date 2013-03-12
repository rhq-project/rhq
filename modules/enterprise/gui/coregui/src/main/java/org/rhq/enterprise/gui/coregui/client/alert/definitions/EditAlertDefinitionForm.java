package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.Map;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertDefinition;

/**
 * @author John Mazzitelli
 */
public interface EditAlertDefinitionForm {

    AlertDefinition getAlertDefinition();
    
//    Map<Integer, AlertCondition> getUpdatedAlertConditions();

    boolean isResetMatching();

    void setAlertDefinition(AlertDefinition alertDef);

    void makeEditable();

    void makeViewOnly();

    void saveAlertDefinition();

    void clearFormValues();
}