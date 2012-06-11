/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.server.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.rest.domain.AlertRest;
import org.rhq.enterprise.server.rest.domain.AlertDefinitionRest;
import org.rhq.enterprise.server.rest.domain.ResourceWithType;

/**
 * Deal with alert related stuff
 * @author Heiko W. Rupp
 */
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class AlertHandlerBean extends AbstractRestBean implements AlertHandlerLocal {

//    private final Log log = LogFactory.getLog(AlertHandlerBean.class);

    @EJB
    AlertManagerLocal alertManager;

    @EJB
    AlertDefinitionManagerLocal alertDefinitionManager;


    @Override
    public List<AlertRest> listAlerts(int page, String status, Request request, UriInfo uriInfo) {


        AlertCriteria criteria = new AlertCriteria();
        criteria.setPaging(page,20); // TODO implement linking to next page
        PageList<Alert> alerts = alertManager.findAlertsByCriteria(caller,criteria);
        List<AlertRest> ret = new ArrayList<AlertRest>(alerts.size());
        for (Alert al : alerts) {
            AlertRest ar = alertToDomain(al, uriInfo);
            ret.add(ar);
        }
        return ret;
    }

    @Override
    public int countAlerts() {
        AlertCriteria criteria = new AlertCriteria();
        criteria.setPageControl(PageControl.getUnlimitedInstance());
        criteria.fetchAlertDefinition(false);
        criteria.fetchConditionLogs(false);
        criteria.fetchRecoveryAlertDefinition(false);
        criteria.fetchNotificationLogs(false);
        criteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        PageList<Alert> alerts = alertManager.findAlertsByCriteria(caller,criteria);
        int count = alerts.getTotalSize();

        return count;
    }

    @Override
    public AlertRest getAlert(int id, UriInfo uriInfo) {

        Alert al = findAlertWithId(id);
        AlertRest ar = alertToDomain(al, uriInfo);
        return ar;
    }



    @Override
    public AlertRest ackAlert(int id, UriInfo uriInfo) {
        findAlertWithId(id); // Ensure the alert exists
        alertManager.acknowledgeAlerts(caller,new int[]{id});
        Alert al = findAlertWithId(id);
        AlertRest ar = alertToDomain(al, uriInfo);
        return ar;
    }

    @Override
    public void purgeAlert(int id) {
        alertManager.deleteAlerts(caller,new int[]{id});

    }

    @Override
    public AlertDefinitionRest getDefinitionForAlert(int alertId) {
        Alert al = findAlertWithId(alertId);
        AlertDefinition def = al.getAlertDefinition();
        AlertDefinitionRest ret = definitionToDomain(def);
        return ret;
    }

    @Override
    public List<AlertDefinitionRest> listAlertDefinitions(int page, String status) {

        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.setPaging(page,20); // TODO add link to next page
        List<AlertDefinition> defs = alertDefinitionManager.findAlertDefinitionsByCriteria(caller,criteria);
        List<AlertDefinitionRest> ret = new ArrayList<AlertDefinitionRest>(defs.size());
        for (AlertDefinition def : defs) {
            AlertDefinitionRest adr = definitionToDomain(def);
            ret.add(adr);
        }
        return ret;
    }

    @Override
    public AlertDefinitionRest getAlertDefinition(int definitionId) {

        AlertDefinition def = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        if (def==null)
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId );
        AlertDefinitionRest adr = definitionToDomain(def);
        return adr;
    }

    @Override
    public AlertDefinitionRest updateDefinition(int definitionId, AlertDefinitionRest definitionRest, Request request) {
        AlertDefinition def = alertDefinitionManager.getAlertDefinition(caller,definitionId);
        if (def==null)
            throw new StuffNotFoundException("AlertDefinition with id " + definitionId);

        def.setEnabled(definitionRest.isEnabled());
        def.setPriority(AlertPriority.valueOf(definitionRest.getPriority()));

        def = alertDefinitionManager.updateAlertDefinition(caller,def.getId(),def,false);

        definitionRest = definitionToDomain(def);

        return definitionRest;
    }

    private AlertDefinitionRest definitionToDomain(AlertDefinition def) {
        AlertDefinitionRest adr = new AlertDefinitionRest(def.getId());
        adr.setName(def.getName());
        adr.setEnabled(def.getEnabled());
        adr.setPriority(def.getPriority().getName());

        return adr;
    }

    /**
     * Retrieve the alert with id id.
     * @param id Primary key of the alert
     * @return Alert domain object
     * @throws StuffNotFoundException if no such alert exists in the system.
     */
    private Alert findAlertWithId(int id) {
        AlertCriteria criteria = new AlertCriteria();
        criteria.addFilterId(id);
        List<Alert> alerts = alertManager.findAlertsByCriteria(caller,criteria);
        if (alerts.isEmpty())
            throw new StuffNotFoundException("Alert with id " + id);

        return alerts.get(0);
    }

    public AlertRest alertToDomain(Alert al, UriInfo uriInfo) {
        AlertRest ret = new AlertRest();
        ret.setId(al.getId());
        AlertDefinition alertDefinition = al.getAlertDefinition();
        ret.setName(alertDefinition.getName());
        ret.setAlertDefinition(definitionToDomain(alertDefinition));
        ret.setDefinitionEnabled(alertDefinition.getEnabled());
        if (al.getAcknowledgingSubject()!=null) {
            ret.setAckBy(al.getAcknowledgingSubject());
            ret.setAckTime(al.getAcknowledgeTime());
        }
        ret.setAlertTime(al.getCtime());

        ret.setDescription(alertManager.prettyPrintAlertConditions(al,false));

        Resource r = fetchResource(alertDefinition.getResource().getId());
        ResourceWithType rwt = fillRWT(r,uriInfo);

        ret.setResource(rwt);

        return ret;
    }
}
