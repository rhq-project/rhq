package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import org.rhq.core.domain.alert.AlertDefinition;

/**
 * @author John Mazzitelli
 */
public interface EditAlertDefinitionForm {

    AlertDefinition getAlertDefinition();

    void setAlertDefinition(AlertDefinition alertDef);

    void makeEditable();

    void makeViewOnly();

    void saveAlertDefinition();

    void clearFormValues();
}