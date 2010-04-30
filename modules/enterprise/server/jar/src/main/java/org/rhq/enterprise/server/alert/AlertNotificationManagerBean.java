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
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertDefinitionContext;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderInfo;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderPluginManager;
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
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private SubjectManagerLocal subjectManager;
    @EJB
    private ServerPluginsLocal serverPluginsBean;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    /*
     * Must use detached object in all circumstances where the AlertDefinition will eventually be passed to
     * AlertDefinitionManager.updateAlertDefinition() to perform the actual modifications and persistence.
     * If we use an attached AlertDefinity entity at this layer, modify the set of notifications, then call
     * into AlertDefinitionManager.updateAlertDefinition() which starts a new transaction, the work will be
     * performed at the AlertTemplate layer twice.  This would result in duplicate notifications (RHQ-629) as
     * well as errors during removal (which would be attempted twice for each being removed).
     *
     * Must, however, return an AlertDefinition with a copy of the ids because the removeNotifications method
     * needs to compare ids to figure out what to remove from the set of notifications.
     */
    private AlertDefinition getDetachedAlertDefinition(int alertDefinitionId) {
        AlertDefinition alertDefinition = alertDefinitionManager.getAlertDefinitionById(subjectManager.getOverlord(),
            alertDefinitionId);
        checkPermission(subjectManager.getOverlord(), alertDefinition);
        AlertDefinitionContext context = alertDefinition.getContext();
        if (context == AlertDefinitionContext.Resource) {
            return alertDefinition; // return attached to make modifications directly on
        }
        // otherwise, return detached to pass to alertTemplateManager.update or groupAlertDefinitionManager.update
        AlertDefinition detachedDefinition = new AlertDefinition(alertDefinition, true);
        detachedDefinition.setContext(context);
        detachedDefinition.setId(alertDefinition.getId());
        return detachedDefinition;
    }

    private void checkPermission(Subject subject, AlertDefinition alertDefinition) {
        boolean hasPermission = false;

        if (alertDefinition.getResourceType() != null) { // an alert template
            hasPermission = authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_SETTINGS);
        } else if (alertDefinition.getResourceGroup() != null) { // a groupAlertDefinition
            hasPermission = authorizationManager.hasGroupPermission(subject, Permission.MANAGE_ALERTS, alertDefinition
                .getResourceGroup().getId());
        } else { // an alert definition
            hasPermission = authorizationManager.hasResourcePermission(subject, Permission.MANAGE_ALERTS,
                alertDefinition.getResource().getId());
        }

        if (!hasPermission) {
            throw new PermissionException(subject + " is not authorized to edit this alert definition");
        }
    }

    public int removeNotifications(Subject subject, Integer alertDefinitionId, Integer[] notificationIds) {
        AlertDefinition alertDefinition = getDetachedAlertDefinition(alertDefinitionId);
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

        // Before we delete the notification, check if has a custom backing bean
        // and give it the possibility to clean up
        for (AlertNotification notification : toBeRemoved) {
            CustomAlertSenderBackingBean bb = getBackingBeanForSender(notification.getSenderName(), notification
                .getId());
            try {
                bb.internalCleanup();
            } catch (Throwable t) {
                LOG
                    .error("removeNotifications, calling backingBean.internalCleanup() resulted in " + t.getMessage(),
                        t);
            }
        }

        alertDefinition.getAlertNotifications().removeAll(toBeRemoved);

        postProcessAlertDefinition(alertDefinition);

        return removed;
    }

    public int purgeOrphanedAlertNotifications() {
        Query purgeQuery = entityManager.createNamedQuery(AlertNotification.QUERY_DELETE_ORPHANED);
        return purgeQuery.executeUpdate();
    }

    private void postProcessAlertDefinition(AlertDefinition definition) {
        AlertDefinitionContext context = definition.getContext();
        if (context == AlertDefinitionContext.Type) {
            try {
                alertTemplateManager.updateAlertTemplate(subjectManager.getOverlord(), definition, true);
            } catch (InvalidAlertDefinitionException iade) {
                // can this ever really happen?  if it does, the logs will know about it
                LOG.error("Can not update alert template, invalid definition: " + definition);
            }
        } else if (context == AlertDefinitionContext.Group) {
            try {
                groupAlertDefintionManager.updateGroupAlertDefinitions(subjectManager.getOverlord(), definition, true);
            } catch (InvalidAlertDefinitionException iade) {
                // can this ever really happen?  if it does, the logs will know about it
                LOG.error("Can not update alert template, invalid definition: " + definition);
            }
        }
    }

    public Configuration getAlertPropertiesConfiguration(AlertNotification notification) {
        Configuration config = notification.getConfiguration();
        if (config != null)
            config = config.deepCopy();

        return config;
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
            ConfigurationDefinition pluginConfigurationDefinition = ConfigurationMetadataParser.parse("alerts:"
                + pluginName, descriptor.getAlertConfiguration());

            return pluginConfigurationDefinition;
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
                Configuration config = notification.getConfiguration();
                Configuration config2 = config.deepCopy(true);

                bean.setAlertParameters(config2);
                try {
                    bean.internalInit();
                } catch (Throwable t) {
                    LOG.error("getBackingBean, calling backingBean.internalInit() resulted in " + t.getMessage());
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
     * {@inheritDoc}
     */
    public AlertNotification addAlertNotification(Subject user, int alertDefinitionId, String senderName,
        Configuration configuration) {

        AlertDefinition definition = alertDefinitionManager.getAlertDefinition(user, alertDefinitionId);
        if (definition == null) {
            LOG.error("Did not find definition for id [" + alertDefinitionId + "]");
            return null;
        }

        entityManager.persist(configuration);
        AlertNotification notif = new AlertNotification(definition);
        notif.setSenderName(senderName);
        notif.setConfiguration(configuration);
        entityManager.persist(notif);
        definition.getAlertNotifications().add(notif);

        return notif;
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
        }

        return notifications;
    }

    /**
     * {@inheritDoc}
     */
    public void updateAlertNotification(AlertNotification notification) {
        notification = entityManager.merge(notification);

        entityManager.persist(notification);
        entityManager.flush();
    }

    public AlertNotification getAlertNotification(Subject user, int alertNotificationId) {
        AlertNotification notification = entityManager.find(AlertNotification.class, alertNotificationId);
        if (notification == null) {
            return null;
        }
        if (notification.getConfiguration() != null) { // an "incomplete" notification might not have a config yet
            notification.getConfiguration().getProperties().size(); // eager load the alert properties
        }
        return notification;
    }

}
