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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.descriptor.configuration.ConfigurationDescriptor;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertDefinitionContext;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.AbstractPropertyMap;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.plugin.ServerPluginManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderInfo;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderPluginManager;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderValidationResults;
import org.rhq.enterprise.server.plugin.pc.alert.CustomAlertSenderBackingBean;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.alert.AlertPluginDescriptorType;

/**
 * @author Joseph Marques
 * @author Heiko W. Rupp
 */

@Stateless
public class AlertNotificationManagerBean implements AlertNotificationManagerLocal {
    private static final Log LOG = LogFactory.getLog(AlertNotificationManagerBean.class);

    @EJB
    private AlertDefinitionManagerLocal alertDefinitionManager;
    @EJB
    private AlertTemplateManagerLocal alertTemplateManager;
    @EJB
    private AlertManagerLocal alertManager;
    @EJB
    private GroupAlertDefinitionManagerLocal groupAlertDefintionManager;
    @EJB
    private SubjectManagerLocal subjectManager;
    @EJB
    private ServerPluginManagerLocal serverPluginsBean;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    /*
     * Must use detached object in all circumstances where the AlertDefinition will eventually be passed to
     * AlertDefinitionManager.updateAlertDefinition() to perform the actual modifications and persistence.
     * If we use an attached AlertDefinity entity at this layer, modify the set of notifications, then call
     * into AlertDefinitionManager.updateAlertDefinition() which executes in a new transaction, the work will
     * actually be performed twice - once at each layer.  This would result in either duplicate notifications
     * (in the case of adding notifications) or Hibernate exceptions (in the case of removing notifications,
     * which are attempted twice for each being removed).
     *
     * However, instead of loading the alertDefinition from within a transaction, and then detaching it from
     * the Hibernate session, a better method is to just execute the add/removal logic for the AlertNotification(s)
     * outside a transaction and then call AlertDefinitionManager.updateAlertNotification() which starts a new
     * transaction to do all of its logic.
     *
     * Note: for AlertNotification updates to work properly, alertDefinitionManager.getAlertDefinitionById() must
     *        eagerly load the List<AlertNotification> on the returned AlertDefinition
     */
    private AlertDefinition getDetachedAlertDefinition(int alertDefinitionId) {
        AlertDefinition alertDefinition = alertDefinitionManager.getAlertDefinitionById(subjectManager.getOverlord(),
            alertDefinitionId);
        return alertDefinition;
    }

    /**
     * @throws AlertDefinitionUpdateException if the {@link AlertNotification} is not associated with a known sender
     */
    @Deprecated
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public AlertNotification addAlertNotification(Subject user, int alertDefinitionId, AlertNotification notification)
        throws AlertDefinitionUpdateException {

        List<String> validSenders = listAllAlertSenders();
        if (validSenders.contains(notification.getSenderName()) == false) {
            throw new AlertDefinitionUpdateException(notification.getSenderName()
                + " is not a valid alert sender, options are: " + validSenders);
        }

        AlertDefinition definition = getDetachedAlertDefinition(alertDefinitionId);
        List<AlertNotification> notifications = definition.getAlertNotifications();
        notifications.add(notification);

        postProcessAlertDefinition(user, definition);

        return notification;
    }

    @Deprecated
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void updateAlertNotification(Subject subject, int alertDefinitionId, AlertNotification notification) {
        AlertDefinition alertDefinition = getDetachedAlertDefinition(alertDefinitionId); // permissions check first

        /*
         * NULL notifications used to perform cascade updates from template and group level for alert senders
         * that leverage custom UIs, which have a completely external methodology for loading/saving the data
         * into and out of configuration object(s) associated with an AlertNotification.
         */
        if (notification != null) {
            // remove then add is a cheap way of performing an update
            List<AlertNotification> notifications = alertDefinition.getAlertNotifications();
            notifications.remove(notification);
            notifications.add(notification);
        }

        postProcessAlertDefinition(subject, alertDefinition);
    }

    @Deprecated
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public int removeNotifications(Subject subject, Integer alertDefinitionId, Integer[] notificationIds) {
        AlertDefinition alertDefinition = getDetachedAlertDefinition(alertDefinitionId); // permissions check first
        if ((notificationIds == null) || (notificationIds.length == 0)) {
            return 0;
        }

        Set<Integer> notificationIdSet = new HashSet<Integer>(Arrays.asList(notificationIds));
        List<AlertNotification> notifications = alertDefinition.getAlertNotifications();
        List<AlertNotification> toBeRemoved = new ArrayList<AlertNotification>();

        int removed = 0;
        for (AlertNotification notification : notifications) {
            if (notificationIdSet.contains(notification.getId())) {
                toBeRemoved.add(notification);
                removed--;
            }
        }

        alertDefinition.getAlertNotifications().removeAll(toBeRemoved);

        postProcessAlertDefinition(subject, alertDefinition);

        return removed;
    }

    public int purgeOrphanedAlertNotifications() {
        Query purgeQuery = entityManager.createNamedQuery(AlertNotification.QUERY_DELETE_ORPHANED);
        return purgeQuery.executeUpdate();
    }

    private AlertDefinition postProcessAlertDefinition(Subject subject, AlertDefinition definition) {
        AlertDefinition updated = null;
        AlertDefinitionContext context = definition.getContext();
        if (context == AlertDefinitionContext.Type) {
            updated = alertTemplateManager.updateAlertTemplate(subject, definition, true);
        } else if (context == AlertDefinitionContext.Group) {
            updated = groupAlertDefintionManager.updateGroupAlertDefinitions(subject, definition, true);
        } else if (context == AlertDefinitionContext.Resource) {
            updated = alertDefinitionManager.updateAlertDefinition(subject, definition.getId(), definition, false);
        } else {
            throw new IllegalStateException("No support for updating alert notifications for AlertDefinitionContext: "
                + definition.getContext());
        }
        return updated;
    }

    public ConfigurationDefinition getConfigurationDefinitionForSender(String shortName) {

        AlertSenderPluginManager pluginmanager = alertManager.getAlertPluginManager();

        AlertSenderInfo senderInfo = pluginmanager.getAlertSenderInfo(shortName);
        String pluginName = senderInfo.getPluginName();
        PluginKey key = senderInfo.getPluginKey();

        try {
            AlertPluginDescriptorType descriptor = (AlertPluginDescriptorType) serverPluginsBean
                .getServerPluginDescriptor(key);
            //ConfigurationDefinition pluginConfigurationDefinition = ConfigurationMetadataParser.parse("pc:" + pluginName, descriptor.getPluginConfiguration());
            ConfigurationDescriptor alertConfiguration = descriptor.getAlertConfiguration();
            if (alertConfiguration==null || alertConfiguration.getConfigurationProperty()== null || alertConfiguration.getConfigurationProperty().isEmpty()) {
                // User either provided no <alert-configuration> or an empty one
                return new ConfigurationDefinition("alerts:"+pluginName,"No properties given");
            }
            else {
                ConfigurationDefinition pluginConfigurationDefinition = ConfigurationMetadataParser.parse("alerts:"
                    + pluginName, alertConfiguration);

                return pluginConfigurationDefinition;
            }
        } catch (Exception e) {
            LOG.error(e);
            return null;
        }
    }

    /**
     * Return a list of all available AlertSenders in the system by their shortname.
     * @return list of senders.
     */
    public List<String> listAllAlertSenders() {
        AlertSenderPluginManager pluginmanager = alertManager.getAlertPluginManager();
        List<String> senders = pluginmanager.getPluginList();
        return senders;
    }

    public AlertSenderInfo getAlertInfoForSender(String shortName) {
        AlertSenderPluginManager pluginmanager = alertManager.getAlertPluginManager();
        AlertSenderInfo info = pluginmanager.getAlertSenderInfo(shortName);

        return info;
    }

    /**
     * Return the backing bean for the AlertSender with the passed shortName. If a notificationId is passed,
     * we try to load the configuration for this notification and pass it to the CustomAlertSenderBackingBean instance
     * @param shortName name of a sender
     * @param alertNotificationId id of the notification we assign this sender + its backing bean to
     * @return an initialized BackingBean or null in case of error
     */
    public CustomAlertSenderBackingBean getBackingBeanForSender(String shortName, Integer alertNotificationId) {

        AlertSenderPluginManager pluginmanager = alertManager.getAlertPluginManager();
        CustomAlertSenderBackingBean bean = pluginmanager.getBackingBeanForSender(shortName);

        if (alertNotificationId != null) {
            AlertNotification notification = entityManager.find(AlertNotification.class, alertNotificationId);
            if (notification != null && bean != null) {
                bean.setAlertParameters(notification.getConfiguration().deepCopy(true));
                if (notification.getExtraConfiguration() != null) {
                    bean.setExtraParameters(notification.getExtraConfiguration().deepCopy(true));
                }
            }
        }
        return bean;
    }

    public String getBackingBeanNameForSender(String shortName) {
        AlertSenderPluginManager pluginmanager = alertManager.getAlertPluginManager();
        return pluginmanager.getBackingBeanNameForSender(shortName);
    }

    /**
     * Return notifications for a certain alertDefinitionId
     *
     * NOTE: this only returns notifications that have an AlertSender defined.
     *
     * @param user Subject of the caller
     * @param alertDefinitionId Id of the alert definition
     * @return list of defined notification of the passed alert definition
     *
     *
     */
    public List<AlertNotification> getNotificationsForAlertDefinition(Subject user, int alertDefinitionId) {
        AlertDefinition definition = alertDefinitionManager.getAlertDefinition(user, alertDefinitionId);
        if (definition == null) {
            LOG.error("Did not find definition for id [" + alertDefinitionId + "]");
            return new ArrayList<AlertNotification>();
        }

        List<AlertNotification> notifications = definition.getAlertNotifications();
        for (AlertNotification notification : notifications) {
            notification.getConfiguration().getProperties().size(); // eager load
            if (notification.getExtraConfiguration() != null) {
                notification.getExtraConfiguration().getProperties().size();
            }
        }

        return notifications;
    }

    public AlertNotification getAlertNotification(Subject user, int alertNotificationId) {
        AlertNotification notification = entityManager.find(AlertNotification.class, alertNotificationId);
        if (notification == null) {
            return null;
        }
        notification.getConfiguration().getProperties().size(); // eager load the alert properties
        if (notification.getExtraConfiguration() != null) {
            notification.getExtraConfiguration().getProperties().size(); // eager load the extra alert properties
        }
        return notification;
    }

    public boolean finalizeNotifications(Subject subject, List<AlertNotification> notifications) {
        boolean hasErrors = false;

        if (notifications != null && notifications.size() > 0) {
            AlertSenderPluginManager pluginManager = alertManager.getAlertPluginManager();

            for (AlertNotification notification : notifications) {
                AlertSender<?> sender = pluginManager.getAlertSenderForNotification(notification);

                if (sender != null) {
                    AlertSenderValidationResults validation = sender.validateAndFinalizeConfiguration(subject);

                    notification.setConfiguration(validation.getAlertParameters());
                    notification.setExtraConfiguration(validation.getExtraParameters());

                    hasErrors = hasErrors || hasErrors(validation.getAlertParameters())
                        || hasErrors(validation.getExtraParameters());
                }
            }
        }

        return !hasErrors;
    }

    public int cleanseAlertNotificationBySubject(int subjectId) {
        return cleanseParameterValueForAlertSender("System Users", "subjectId", String.valueOf(subjectId));
    }

    public int cleanseAlertNotificationByRole(int roleId) {
        return cleanseParameterValueForAlertSender("System Roles", "roleId", String.valueOf(roleId));
    }

    public void massReconfigure(List<Integer> alertNotificationIds, Map<String, String> newConfigurationValues) {
        Query query = entityManager.createNamedQuery(AlertNotification.QUERY_UPDATE_PARAMETER_FOR_NOTIFICATIONS);

        query.setParameter("alertNotificationIds", alertNotificationIds);

        for (Map.Entry<String, String> entry : newConfigurationValues.entrySet()) {
            query.setParameter("propertyName", entry.getKey());
            query.setParameter("propertyValue", entry.getValue());

            query.executeUpdate();
        }
    }

    private int cleanseParameterValueForAlertSender(String senderName, String propertyName, String valueToCleanse) {
        Query query = entityManager.createNamedQuery(AlertNotification.QUERY_CLEANSE_PARAMETER_VALUE_FOR_ALERT_SENDER);
        query.setParameter("senderName", senderName);
        query.setParameter("propertyName", propertyName);
        query.setParameter("paramValue", "|" + valueToCleanse + "|"); // wrap with fence-delimiter for search
        int affectedRows = query.executeUpdate();
        return affectedRows;
    }

    private boolean hasErrors(AbstractPropertyMap configuration) {
        if (configuration instanceof PropertyMap) {
            if (((PropertyMap) configuration).getErrorMessage() != null) {
                return true;
            }
        }

        for (Map.Entry<String, Property> entry : configuration.getMap().entrySet()) {
            if (hasErrors(entry.getValue())) {
                return true;
            }
        }

        return false;
    }

    private boolean hasErrors(PropertyList list) {
        if (list.getErrorMessage() != null) {
            return true;
        }

        for (Property p : list.getList()) {
            if (hasErrors(p)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasErrors(Property property) {
        if (property instanceof PropertySimple) {
            return property.getErrorMessage() != null;
        } else if (property instanceof PropertyList) {
            return hasErrors((PropertyList) property);
        } else if (property instanceof PropertyMap) {
            return hasErrors((AbstractPropertyMap) property);
        } else {
            return false;
        }
    }
}
