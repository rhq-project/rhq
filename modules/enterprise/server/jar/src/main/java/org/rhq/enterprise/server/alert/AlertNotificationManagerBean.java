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
import java.util.Collections;
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
import org.rhq.core.clientapi.descriptor.configuration.ConfigurationDescriptor;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertDefinitionContext;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.alert.notification.EmailNotification;
import org.rhq.core.domain.alert.notification.RoleNotification;
import org.rhq.core.domain.alert.notification.SnmpNotification;
import org.rhq.core.domain.alert.notification.SubjectNotification;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.configuration.metadata.ConfigurationMetadataManagerLocal;
import org.rhq.enterprise.server.plugin.ServerPluginsLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderInfo;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderPluginManager;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.alert.AlertPluginDescriptorType;

/**
 * @author Joseph Marques
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
    private RoleManagerLocal roleManager;
    @EJB
    private SubjectManagerLocal subjectManager;
    @EJB
    private ConfigurationMetadataManagerLocal confMeMan;
    @EJB
    private ServerPluginsLocal serverPluginsBean;
    @EJB
    private ConfigurationManagerLocal configManager;

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

        if (hasPermission == false) {
            throw new PermissionException(subject + " is not authorized to edit this alert definition");
        }
    }

    public int addEmailNotifications(Subject subject, Integer alertDefinitionId, String[] emails) {
        AlertDefinition alertDefinition = getDetachedAlertDefinition(alertDefinitionId);
        Set<AlertNotification> notifications = alertDefinition.getAlertNotifications();

        int added = 0;
        for (String emailAddress : emails) {
            emailAddress = emailAddress.toLowerCase().trim();
            if (emailAddress.equals("")) {
                continue; // don't add empty addresses
            }
            EmailNotification notification = new EmailNotification(alertDefinition, emailAddress);

            // only increment for non-duplicate additions
            if (notifications.contains(notification) == false) {
                added++;
                notifications.add(notification); // cascading should take care of persisting
            }
        }

        postProcessAlertDefinition(alertDefinition);

        return added;
    }

    @SuppressWarnings("unchecked")
    public PageList<EmailNotification> getEmailNotifications(Integer alertDefinitionId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("en.emailAddress");

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            EmailNotification.QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            EmailNotification.QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID, pageControl);

        queryCount.setParameter("alertDefinitionId", alertDefinitionId);
        query.setParameter("alertDefinitionId", alertDefinitionId);

        long count = (Long) queryCount.getSingleResult();
        List<EmailNotification> results = query.getResultList();

        return new PageList<EmailNotification>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<EmailNotification> getEmailNotifications(Integer[] alertNotificationIds, PageControl pageControl) {
        pageControl.initDefaultOrderingField("en.emailAddress");

        if ((alertNotificationIds == null) || (alertNotificationIds.length == 0)) {
            return new PageList<EmailNotification>(Collections.EMPTY_LIST, 0, pageControl);
        }

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, EmailNotification.QUERY_FIND_BY_IDS);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, EmailNotification.QUERY_FIND_BY_IDS,
            pageControl);

        queryCount.setParameter("ids", alertNotificationIds);
        query.setParameter("ids", alertNotificationIds);

        long count = (Long) queryCount.getSingleResult();
        List<EmailNotification> results = query.getResultList();

        return new PageList<EmailNotification>(results, (int) count, pageControl);
    }

    public int addRoleNotifications(Subject subject, Integer alertDefinitionId, Integer[] roleIds) {
        AlertDefinition alertDefinition = getDetachedAlertDefinition(alertDefinitionId);
        Set<AlertNotification> notifications = alertDefinition.getAlertNotifications();

        List<Role> roles = roleManager.findRolesByIds(roleIds, PageControl.getUnlimitedInstance());

        int added = 0;
        for (Role role : roles) {
            RoleNotification notification = new RoleNotification(alertDefinition, role);

            // only increment for non-duplicate additions
            if (notifications.contains(notification) == false) {
                added++;
                notifications.add(notification); // cascading should take care of persisting
            }
        }

        postProcessAlertDefinition(alertDefinition);

        return added;
    }

    @SuppressWarnings("unchecked")
    public PageList<RoleNotification> getRoleNotifications(Integer alertDefinitionId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("rn.role.name");

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            RoleNotification.QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            RoleNotification.QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID, pageControl);

        queryCount.setParameter("alertDefinitionId", alertDefinitionId);
        query.setParameter("alertDefinitionId", alertDefinitionId);

        long count = (Long) queryCount.getSingleResult();
        List<RoleNotification> results = query.getResultList();

        return new PageList<RoleNotification>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<RoleNotification> getRoleNotifications(Integer[] alertNotificationIds, PageControl pageControl) {
        pageControl.initDefaultOrderingField("rn.role.name");

        if ((alertNotificationIds == null) || (alertNotificationIds.length == 0)) {
            return new PageList<RoleNotification>(Collections.EMPTY_LIST, 0, pageControl);
        }

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, RoleNotification.QUERY_FIND_BY_IDS);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, RoleNotification.QUERY_FIND_BY_IDS,
            pageControl);

        queryCount.setParameter("ids", alertNotificationIds);
        query.setParameter("ids", alertNotificationIds);

        long count = (Long) queryCount.getSingleResult();
        List<RoleNotification> results = query.getResultList();

        return new PageList<RoleNotification>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<RoleNotification> getRoleNotificationsByRoles(Integer[] roleIds, PageControl pageControl) {
        pageControl.initDefaultOrderingField("rn.role.name");

        if ((roleIds == null) || (roleIds.length == 0)) {
            return new PageList<RoleNotification>(Collections.EMPTY_LIST, 0, pageControl);
        }

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, RoleNotification.QUERY_FIND_BY_ROLE_IDS);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, RoleNotification.QUERY_FIND_BY_ROLE_IDS,
            pageControl);

        queryCount.setParameter("ids", roleIds);
        query.setParameter("ids", roleIds);

        long count = (Long) queryCount.getSingleResult();
        List<RoleNotification> results = query.getResultList();

        return new PageList<RoleNotification>(results, (int) count, pageControl);
    }

    public int addSubjectNotifications(Subject user, Integer alertDefinitionId, Integer[] subjectIds) {
        AlertDefinition alertDefinition = getDetachedAlertDefinition(alertDefinitionId);
        Set<AlertNotification> notifications = alertDefinition.getAlertNotifications();

        List<Subject> subjects = subjectManager.findSubjectsById(subjectIds, PageControl.getUnlimitedInstance());

        int added = 0;
        for (Subject subject : subjects) {
            SubjectNotification notification = new SubjectNotification(alertDefinition, subject);

            // only increment for non-duplicate additions
            if (notifications.contains(notification) == false) {
                added++;
                notifications.add(notification); // cascading should take care of persisting
            }
        }

        postProcessAlertDefinition(alertDefinition);

        return added;
    }

    @SuppressWarnings("unchecked")
    public PageList<SubjectNotification> getSubjectNotifications(Integer alertDefinitionId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("sn.subject.name");

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            SubjectNotification.QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            SubjectNotification.QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID, pageControl);

        queryCount.setParameter("alertDefinitionId", alertDefinitionId);
        query.setParameter("alertDefinitionId", alertDefinitionId);

        long count = (Long) queryCount.getSingleResult();
        List<SubjectNotification> results = query.getResultList();

        return new PageList<SubjectNotification>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<SnmpNotification> getSnmpNotifications(Integer alertDefinitionId, PageControl pageControl) {
        pageControl.initDefaultOrderingField("sn.host");

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            SnmpNotification.QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            SnmpNotification.QUERY_FIND_ALL_BY_ALERT_DEFINITION_ID, pageControl);

        queryCount.setParameter("alertDefinitionId", alertDefinitionId);
        query.setParameter("alertDefinitionId", alertDefinitionId);

        long count = (Long) queryCount.getSingleResult();
        List<SnmpNotification> results = query.getResultList();

        return new PageList<SnmpNotification>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<SubjectNotification> getSubjectNotifications(Integer[] alertNotificationIds, PageControl pageControl) {
        pageControl.initDefaultOrderingField("sn.subject.name");

        if ((alertNotificationIds == null) || (alertNotificationIds.length == 0)) {
            return new PageList<SubjectNotification>(Collections.EMPTY_LIST, 0, pageControl);
        }

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, SubjectNotification.QUERY_FIND_BY_IDS);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, SubjectNotification.QUERY_FIND_BY_IDS,
            pageControl);

        queryCount.setParameter("ids", alertNotificationIds);
        query.setParameter("ids", alertNotificationIds);

        long count = (Long) queryCount.getSingleResult();
        List<SubjectNotification> results = query.getResultList();

        return new PageList<SubjectNotification>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<SubjectNotification> getSubjectNotificationsBySubjects(Integer[] subjectIds, PageControl pageControl) {
        pageControl.initDefaultOrderingField("rn.role.name");

        if ((subjectIds == null) || (subjectIds.length == 0)) {
            return new PageList<SubjectNotification>(Collections.EMPTY_LIST, 0, pageControl);
        }

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            SubjectNotification.QUERY_FIND_BY_SUBJECT_IDS);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            SubjectNotification.QUERY_FIND_BY_SUBJECT_IDS, pageControl);

        queryCount.setParameter("ids", subjectIds);
        query.setParameter("ids", subjectIds);

        long count = (Long) queryCount.getSingleResult();
        List<SubjectNotification> results = query.getResultList();

        return new PageList<SubjectNotification>(results, (int) count, pageControl);
    }

    public int removeNotifications(Subject subject, Integer alertDefinitionId, Integer[] notificationIds) {
        AlertDefinition alertDefinition = getDetachedAlertDefinition(alertDefinitionId);
        if ((notificationIds == null) || (notificationIds.length == 0)) {
            return 0;
        }

        Set<Integer> notificationIdSet = new HashSet<Integer>(Arrays.asList(notificationIds));
        List<AlertNotification> notifications = new ArrayList<AlertNotification>(alertDefinition
            .getAlertNotifications());
        List<AlertNotification> toBeRemoved = new ArrayList<AlertNotification>();

        int removed = 0;
        for (AlertNotification notification : notifications) {
            if (notificationIdSet.contains(notification.getId())) {
                toBeRemoved.add(notification);
                removed--;
            }
        }

        alertDefinition.getAlertNotifications().removeAll(toBeRemoved);

        postProcessAlertDefinition(alertDefinition);

        return removed;
    }

    public void setSnmpNotification(Subject subject, Integer alertDefinitionId, SnmpNotification snmpNotification) {
        AlertDefinition alertDefinition = getDetachedAlertDefinition(alertDefinitionId);
        Set<AlertNotification> alertNotifications = alertDefinition.getAlertNotifications();
        for (AlertNotification alertNotification : alertNotifications) {
            if (alertNotification instanceof SnmpNotification) {
                alertNotifications.remove(alertNotification);
            }
        }

        alertNotifications.add(snmpNotification);

        postProcessAlertDefinition(alertDefinition);
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
        if (config!=null)
                config = config.deepCopy();

        return config;
    }

    public ConfigurationDefinition getConfigurationDefinitionForSender(String shortName) {

        AlertSenderPluginManager pluginmanager = alertManager.getAlertPluginManager();

        AlertSenderInfo senderInfo = pluginmanager.getAlertSenderInfo(shortName);
        String pluginName = senderInfo.getPluginName();
        PluginKey key = senderInfo.getPluginKey();

        try {
            AlertPluginDescriptorType descriptor = (AlertPluginDescriptorType) serverPluginsBean.getServerPluginDescriptor(key);
            //ConfigurationDefinition pluginConfigurationDefinition = ConfigurationMetadataParser.parse("pc:" + pluginName, descriptor.getPluginConfiguration());
            ConfigurationDefinition pluginConfigurationDefinition = ConfigurationMetadataParser.parse("alerts:" + pluginName, descriptor.getAlertConfiguration());


            return pluginConfigurationDefinition;
        }
        catch (Exception e) {
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
     * Add a new AlertNotification to the passed definition
     * @param user subject of the caller
     * @param alertDefinitionId Id of the alert definition
     * @param senderName shortName of the {@link AlertSender}
     * @param configuration Properties for this alert sender.
     */
    public void addAlertNotification(Subject user, int alertDefinitionId, String senderName, Configuration configuration) {

        AlertDefinition definition = alertDefinitionManager.getAlertDefinition(user,alertDefinitionId);
        if (definition==null) {
            LOG.error("DId not find definition for id [" + alertDefinitionId+ "]");
            return;
        }

        entityManager.persist(configuration);
        AlertNotification notif = new AlertNotification(definition);
        notif.setSenderName(senderName);
        notif.setConfiguration(configuration);
        entityManager.persist(notif);
        definition.getAlertNotifications().add(notif);

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
        AlertDefinition definition = alertDefinitionManager.getAlertDefinition(user,alertDefinitionId);
        if (definition==null) {
            LOG.error("DId not find definition for id [" + alertDefinitionId+ "]");
            return new ArrayList<AlertNotification>();
        }
        Set<AlertNotification> notifs = definition.getAlertNotifications();
        List<AlertNotification> result = new ArrayList<AlertNotification>();
        for (AlertNotification notif : notifs) {
            if (notif.getSenderName()!=null) {
                notif.getConfiguration().getProperties().size(); // Eager load
                result.add(notif);
            }
        }
        return result;
    }
}