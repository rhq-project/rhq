/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.alert;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertDefinitionUpdateException;
import org.rhq.enterprise.server.alert.InvalidAlertDefinitionException;

/**
 *
 * @author Justin Harris
 */
@Scope(ScopeType.PAGE)
@Name("alertUIBean")
public class AlertUIBean implements Serializable {

    @Logger
    private Log log;
    @In("#{webUser.subject}")
    private Subject subject;
    @In
    private FacesMessages facesMessages;
    @In
    private AlertDescriber alertDescriber;
    @In
    private AlertDefinitionManagerLocal alertDefinitionManager;
    @In
    private AlertDefinition alertDefinition;
    private List<String> alertConditions;
    private String alertDampening;

    public String getAlertDampening() {
        return this.alertDampening;
    }

    public AlertDefinition getAlertDefinition() {
        return alertDefinition;
    }

    public void setAlertDefinition(AlertDefinition alertDefinition) {
        this.alertDefinition = alertDefinition;
    }

    public List<String> getAlertConditions() {
        return alertConditions;
    }

    public String saveAlertDefinition() {
        try {
            alertDefinitionManager.updateAlertDefinition(this.subject,
                    this.alertDefinition.getId(), this.alertDefinition, false);
        } catch (InvalidAlertDefinitionException e) {
            facesMessages.add("There was an error finding the requested alert definition.");
            log.error("Invalid alert definition:  " + this.alertDefinition.toSimpleString(), e);
        } catch (AlertDefinitionUpdateException e) {
            facesMessages.add("There was an error updating the definition for " + this.alertDefinition.getName());
            log.error("Error updating alert definition:  " + this.alertDefinition.toSimpleString(), e);
        }

        return null;
    }

    @Create
    public void init() {
        this.alertConditions = lookupAlertConditions();
        this.alertDampening = lookupAlertDampening();
    }

    private List<String> lookupAlertConditions() {
        List<String> conditions = new ArrayList<String>();

        for (AlertCondition condition : this.alertDefinition.getConditions()) {
            conditions.add(this.alertDescriber.describeCondition(condition));
        }

        return conditions;
    }

    private String lookupAlertDampening() {
        AlertDampening dampening = this.alertDefinition.getAlertDampening();

        return this.alertDescriber.describeDampening(dampening);
    }
}