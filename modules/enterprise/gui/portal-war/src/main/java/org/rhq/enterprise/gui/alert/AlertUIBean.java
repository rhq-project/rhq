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


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.faces.model.SelectItem;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertDefinitionUpdateException;
import org.rhq.enterprise.server.alert.InvalidAlertDefinitionException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 *
 * @author Justin Harris
 */
@Scope(ScopeType.PAGE)
@Name("alertUIBean")
public class AlertUIBean {

    @Logger
    private Log log;
    @In
    private FacesMessages facesMessages;
    @In
    private AlertDescriber alertDescriber;
    @DataModel(scope=ScopeType.PAGE)
    private List<String> alertConditions;
    private AlertDefinition alertDefinition;
    private String alertDampening;
    private String dateCreated;
    private String dateModified;

    private DateFormat formatter;

    public AlertUIBean() {
        this.formatter = new SimpleDateFormat("M/d/yy h:mm:ss aa");
    }

    public String getAlertDampening() {
        return this.alertDampening;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public String getDateModified() {
        return dateModified;
    }

    public AlertDefinition getAlertDefinition() {
        return alertDefinition;
    }

    public void setAlertDefinition(AlertDefinition alertDefinition) {
        this.alertDefinition = alertDefinition;
    }


    public List<SelectItem> getPriorities() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        for (AlertPriority priority : AlertPriority.values()) {
            items.add(new SelectItem(priority.name(), priority.getDisplayName()));
        }

        return items;
    }

    public String saveAlertDefinition() {
        // TODO:  Use dependency injection to look these up
        AlertDefinitionManagerLocal definitionManager = LookupUtil.getAlertDefinitionManager();
        Subject subject = EnterpriseFacesContextUtility.getSubject();

        try {
            definitionManager.updateAlertDefinition(subject, this.alertDefinition.getId(), this.alertDefinition, false);
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
        // Look this up from the Seam context instead of DI - this way the form submission updates
        // the defintion's values properly
        this.alertDefinition = (AlertDefinition) Component.getInstance("alertDefinition");

        lookupDates();
        lookupAlertConditions();
        lookupAlertDampening();
    }

    private void lookupDates() {
        this.dateCreated = formatDate(this.alertDefinition.getCtime());
        this.dateModified = formatDate(this.alertDefinition.getMtime());
    }

    private void lookupAlertConditions() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        this.alertConditions = new ArrayList<String>();

        for (AlertCondition condition : this.alertDefinition.getConditions()) {
            this.alertConditions.add(this.alertDescriber.describeCondition(condition));
        }
    }

    private void lookupAlertDampening() {
        AlertDampening dampening = this.alertDefinition.getAlertDampening();
        this.alertDampening = this.alertDescriber.describeDampening(dampening);
    }

    private String formatDate(long timestamp) {
        return formatter.format(new Date(timestamp));
    }
}