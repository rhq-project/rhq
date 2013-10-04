package org.rhq.coregui.client.alert.definitions;

import org.rhq.core.domain.alert.AlertDefinition;

/**
 * @author John Mazzitelli
 */
public interface EditAlertDefinitionForm {

    AlertDefinition getAlertDefinition();
    
    boolean isResetMatching();

    void setAlertDefinition(AlertDefinition alertDef);

    void makeEditable();

    void makeViewOnly();

    void saveAlertDefinition();

    void clearFormValues();
}