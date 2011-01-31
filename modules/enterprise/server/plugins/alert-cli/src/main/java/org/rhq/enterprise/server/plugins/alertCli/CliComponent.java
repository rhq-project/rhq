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

package org.rhq.enterprise.server.plugins.alertCli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.util.DisambiguationReportRenderer;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.util.IntExtractor;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.plugin.pc.ControlFacet;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.disambiguation.DefaultDisambiguationUpdateStrategies;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The plugin component for controlling the CLI alerts.
 *
 * @author Lukas Krejci
 */
public class CliComponent implements ServerPluginComponent, ControlFacet {

    private static final String CONTROL_CHECK_ALERTS_VALIDITY = "checkAlertsValidity";
    private static final String CONTROL_REASSIGN_ALERTS = "reassignAlerts";
    
    private static final String PROP_PACKAGE_TYPES = "packageTypes";
    private static final String PROP_ALERT_DEFINITION_NAME = "alertDefinitionName";
    private static final String PROP_RESOURCE_PATH = "resourcePath";
    private static final String PROP_RESOURCE_ID = "resourceId";
    private static final String PROP_MISCONFIGURED_ALERT_DEFS = "misconfiguredAlertDefs";
    private static final String PROP_ALERT_DEFINITION = "alertDefinition";
    private static final String PROP_ALERT_DEFINITION_ID = "alertDefinitionId";
    private static final String PROP_USER_NAME = "userName";
    private static final String PROP_ALERT_DEF_IDS = "alertDefIds";
    
    private Set<String> scriptPackageTypes;
    private String pluginName;
    
    public void initialize(ServerPluginContext context) throws Exception {
        scriptPackageTypes = new HashSet<String>();
        pluginName = context.getPluginEnvironment().getPluginDescriptor().getName();
        PropertyList packageTypesList = context.getPluginConfiguration().getList(PROP_PACKAGE_TYPES);
        for(Property p : packageTypesList.getList()) {
            PropertySimple value = (PropertySimple) p;
            
            scriptPackageTypes.add(value.getStringValue());
        }
    }

    public void start() {
    }

    public void stop() {
    }

    public void shutdown() {
    }

    public ControlResults invoke(String name, Configuration parameters) {
        ControlResults results = new ControlResults();
        
        try {
            if (CONTROL_CHECK_ALERTS_VALIDITY.equals(name)) {
                checkAlertsValidity(results, parameters);
            } else if (CONTROL_REASSIGN_ALERTS.equals(name)) {
                reassignAlerts(results, parameters);
            }
        } catch (Exception e) {
            results.setError(e);
        }
        
        return results;
    }
    
    private void checkAlertsValidity(ControlResults results, Configuration parameters) {
        List<AlertNotification> invalidNotifs = getAllCliNotificationsWithInvalidUser();
        
        for(AlertNotification cliNotification : invalidNotifs) {
            AlertDefinition def = cliNotification.getAlertDefinition();
            
            Configuration resConfig = results.getComplexResults();
            
            PropertyList misconfigured = resConfig.getList(PROP_MISCONFIGURED_ALERT_DEFS);
            if (misconfigured == null) {
                misconfigured = new PropertyList(PROP_MISCONFIGURED_ALERT_DEFS);
                resConfig.put(misconfigured);
            }
            
            PropertyMap alertDefinitionMap = new PropertyMap(PROP_ALERT_DEFINITION);
            
            alertDefinitionMap.put(new PropertySimple(PROP_ALERT_DEFINITION_ID, def.getId()));
            alertDefinitionMap.put(new PropertySimple(PROP_ALERT_DEFINITION_NAME, def.getName()));
            alertDefinitionMap.put(new PropertySimple(PROP_RESOURCE_ID, def.getResource().getId()));
            
            misconfigured.add(alertDefinitionMap);
        }
        
        //ok, now we have to obtain the resource paths. doing it out of the above loop reduces the number
        //of server roundtrips
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        PropertyList misconfigured = results.getComplexResults().getList(PROP_MISCONFIGURED_ALERT_DEFS);
        if (misconfigured != null) {
            List<DisambiguationReport<Property>> disambiguated = resourceManager.disambiguate(misconfigured.getList(), 
                new IntExtractor<Property>() {
                    public int extract(Property object) {
                        PropertyMap map = (PropertyMap) object;
                        return map.getSimple(PROP_RESOURCE_ID).getIntegerValue();
                    };
                }, 
                DefaultDisambiguationUpdateStrategies.KEEP_ALL_PARENTS);
            
            DisambiguationReportRenderer renderer = new DisambiguationReportRenderer();
            
            for(DisambiguationReport<Property> r : disambiguated) {
                PropertyMap map = (PropertyMap) r.getOriginal();
                
                String resourcePath = renderer.render(r);
                
                map.put(new PropertySimple(PROP_RESOURCE_PATH, resourcePath));
            }
        }
        
    }
    
    private void reassignAlerts(ControlResults results, Configuration parameters) {
        PropertySimple userNameProp = parameters.getSimple(PROP_USER_NAME);
        PropertySimple alertDefIdsProp = parameters.getSimple(PROP_ALERT_DEF_IDS);
        
        if (userNameProp == null) {
            throw new IllegalArgumentException("User name not specified.");
        }
        
        String userName = userNameProp.getStringValue();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject subject = subjectManager.getSubjectByName(userName);
        
        if (subject == null) {
            throw new IllegalArgumentException("User '" + userName + "' doesn't exist.");
        }
        
        //now get the list of the alert notifications to update
        List<AlertNotification> notifsToReAssign = null;
        if (alertDefIdsProp == null || alertDefIdsProp.getStringValue().trim().length() == 0) {
            notifsToReAssign = getAllCliNotificationsWithInvalidUser();
        } else {
            List<Integer> alertDefIds = asIdList(alertDefIdsProp.getStringValue().split("\\s*,\\s*"));
            List<AlertDefinition> defs = getAlertDefinitionsWithCliNotifications(alertDefIds);
            
            notifsToReAssign = new ArrayList<AlertNotification>();
            for(AlertDefinition def : defs) {
                AlertNotification cliNotif = getCliNotification(def.getAlertNotifications());
                if (cliNotif != null) {
                    notifsToReAssign.add(cliNotif);
                }
            }
        }
        
        AlertNotificationManagerLocal notificationManager = LookupUtil.getAlertNotificationManager();

        List<Integer> notifIds = asIdList(notifsToReAssign);
        Map<String, String> updates = Collections.singletonMap(CliSender.PROP_USER_ID, Integer.toString(subject.getId()));
        notificationManager.massReconfigure(notifIds, updates);
    }
    
    private AlertNotification getCliNotification(List<AlertNotification> notifications) {
        for(AlertNotification n : notifications) {
            if (pluginName.equals(n.getSenderName())) {
                return n;
            }
        }
        
        return null;
    }
    
    private List<AlertDefinition> getAlertDefinitionsWithCliNotifications(Collection<Integer> ids) {
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        AlertDefinitionManagerLocal manager = LookupUtil.getAlertDefinitionManager();

        Subject overlord = subjectManager.getOverlord();

        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.addFilterNotificationNames(pluginName);
        criteria.setPageControl(PageControl.getUnlimitedInstance());
        criteria.fetchAlertNotifications(true);
        if (ids != null) {
            criteria.addFilterIds(ids.toArray(new Integer[ids.size()]));
        }
        
        return manager.findAlertDefinitionsByCriteria(overlord, criteria);
    }
    
    private List<AlertNotification> getAllCliNotificationsWithInvalidUser() {
        List<AlertNotification> ret = new ArrayList<AlertNotification>();
        
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        
        List<AlertDefinition> defs = getAlertDefinitionsWithCliNotifications(null);
        
        for(AlertDefinition def : defs) {
            List<AlertNotification> notifications = def.getAlertNotifications();
            
            AlertNotification cliNotification = getCliNotification(notifications);
            
            if (cliNotification == null) {
                //we always should find this but a little bit of paranoia never hurt anyone
                continue;
            }
            
            Subject checkSubject = null;
            
            PropertySimple subjectIdProperty = cliNotification.getConfiguration().getSimple(CliSender.PROP_USER_ID);
            if (subjectIdProperty != null) {                
                int subjectId = subjectIdProperty.getIntegerValue();
                
                checkSubject = subjectManager.getSubjectById(subjectId);
            }
            
            if (checkSubject == null) {
                ret.add(cliNotification);
            }
        }
        
        return ret;
    }
    
    private List<Integer> asIdList(String... ids) {
        List<Integer> ret = new ArrayList<Integer>();
        for(String id : ids) {
            ret.add(Integer.parseInt(id));
        }
        
        return ret;
    }
    
    private List<Integer> asIdList(Collection<AlertNotification> notifs) {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        
        for(AlertNotification n : notifs) {
            ret.add(n.getId());
        }
        
        return ret;
    }
}
