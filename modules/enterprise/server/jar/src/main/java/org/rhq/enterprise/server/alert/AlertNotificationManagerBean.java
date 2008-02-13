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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.annotation.IgnoreDependency;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.event.alert.AlertDefinition;
import org.rhq.core.domain.event.alert.notification.AlertNotification;
import org.rhq.core.domain.event.alert.notification.EmailNotification;
import org.rhq.core.domain.event.alert.notification.RoleNotification;
import org.rhq.core.domain.event.alert.notification.SnmpNotification;
import org.rhq.core.domain.event.alert.notification.SubjectNotification;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RoleManagerLocal;

@Stateless
public class AlertNotificationManagerBean implements AlertNotificationManagerLocal {
    private static final Log LOG = LogFactory.getLog(AlertNotificationManagerBean.class);

    @EJB
    AlertDefinitionManagerLocal alertDefinitionManager;
    @EJB
    @IgnoreDependency
    AlertTemplateManagerLocal alertTemplateManager;
    @EJB
    AuthorizationManagerLocal authorizationManager;
    @EJB
    RoleManagerLocal roleManager;
    @EJB
    SubjectManagerLocal subjectManager;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    private void checkPermission(Subject subject, AlertDefinition alertDefinition, boolean isAlertTemplate) {
        boolean hasPermission = false;

        if (isAlertTemplate) {
            hasPermission = authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_INVENTORY);
        } else {
            hasPermission = authorizationManager.hasResourcePermission(subject, Permission.MANAGE_ALERTS,
                alertDefinition.getResource().getId());
        }

        if (hasPermission == false) {
            throw new PermissionException(subject + " is not authorized to edit this alert definition");
        }
    }

    @SuppressWarnings("unchecked")
    public int addEmailNotifications(Subject subject, Integer alertDefinitionId, String[] emails,
        boolean isAlertTemplate) {
        AlertDefinition alertDefinition = alertDefinitionManager.getAlertDefinitionById(subjectManager.getOverlord(),
            alertDefinitionId);
        checkPermission(subject, alertDefinition, isAlertTemplate);

        Set<AlertNotification> notifications = alertDefinition.getAlertNotifications();

        int added = 0;
        for (String emailAddress : emails) {
            emailAddress = emailAddress.toLowerCase();
            EmailNotification notification = new EmailNotification(alertDefinition, emailAddress);

            // only increment for non-duplicate additions
            if (notifications.contains(notification) == false) {
                added++;
                notifications.add(notification); // cascading should take care of persisting
            }
        }

        if (isAlertTemplate) {
            try {
                alertTemplateManager.updateAlertTemplate(subjectManager.getOverlord(), alertDefinition, true);
            } catch (InvalidAlertDefinitionException iade) {
                // can this ever really happen?  if it does, the logs will know about it
                LOG.error("Can not update alert template, invalid definition: " + alertDefinition);
            }
        }

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

    @SuppressWarnings("unchecked")
    public int addRoleNotifications(Subject subject, Integer alertDefinitionId, Integer[] roleIds,
        boolean isAlertTemplate) {
        AlertDefinition alertDefinition = alertDefinitionManager.getAlertDefinitionById(subjectManager.getOverlord(),
            alertDefinitionId);
        checkPermission(subject, alertDefinition, isAlertTemplate);

        Set<AlertNotification> notifications = alertDefinition.getAlertNotifications();

        List<Role> roles = roleManager.getRolesById(roleIds, PageControl.getUnlimitedInstance());

        int added = 0;
        for (Role role : roles) {
            RoleNotification notification = new RoleNotification(alertDefinition, role);

            // only increment for non-duplicate additions
            if (notifications.contains(notification) == false) {
                added++;
                notifications.add(notification); // cascading should take care of persisting
            }
        }

        if (isAlertTemplate) {
            try {
                alertTemplateManager.updateAlertTemplate(subjectManager.getOverlord(), alertDefinition, true);
            } catch (InvalidAlertDefinitionException iade) {
                // can this ever really happen?  if it does, the logs will know about it
                LOG.error("Can not update alert template, invalid definition: " + alertDefinition);
            }
        }

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

    @SuppressWarnings("unchecked")
    public int addSubjectNotifications(Subject user, Integer alertDefinitionId, Integer[] subjectIds,
        boolean isAlertTemplate) {
        AlertDefinition alertDefinition = alertDefinitionManager.getAlertDefinitionById(subjectManager.getOverlord(),
            alertDefinitionId);
        checkPermission(user, alertDefinition, isAlertTemplate);

        Set<AlertNotification> notifications = alertDefinition.getAlertNotifications();

        List<Subject> subjects = subjectManager.getSubjectsById(subjectIds, PageControl.getUnlimitedInstance());

        int added = 0;
        for (Subject subject : subjects) {
            SubjectNotification notification = new SubjectNotification(alertDefinition, subject);

            // only increment for non-duplicate additions
            if (notifications.contains(notification) == false) {
                added++;
                notifications.add(notification); // cascading should take care of persisting
            }
        }

        if (isAlertTemplate) {
            try {
                alertTemplateManager.updateAlertTemplate(subjectManager.getOverlord(), alertDefinition, true);
            } catch (InvalidAlertDefinitionException iade) {
                // can this ever really happen?  if it does, the logs will know about it
                LOG.error("Can not update alert template, invalid definition: " + alertDefinition);
            }
        }

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

    public int removeNotifications(Subject subject, Integer alertDefinitionId, Integer[] notificationIds,
        boolean isAlertTemplate) {
        AlertDefinition alertDefinition = alertDefinitionManager.getAlertDefinitionById(subjectManager.getOverlord(),
            alertDefinitionId);
        checkPermission(subject, alertDefinition, isAlertTemplate);

        if ((notificationIds == null) || (notificationIds.length == 0)) {
            return 0;
        }

        Query query = entityManager.createNamedQuery(AlertNotification.DELETE_BY_ID);
        query.setParameter("ids", Arrays.asList(notificationIds));
        int recordsRemoved = query.executeUpdate();

        return recordsRemoved;
    }

    public void setSnmpNotification(Subject subject, Integer alertDefinitionId, SnmpNotification snmpNotification,
        boolean isAlertTemplate) {
        AlertDefinition alertDefinition = alertDefinitionManager.getAlertDefinitionById(subjectManager.getOverlord(),
            alertDefinitionId);
        checkPermission(subject, alertDefinition, isAlertTemplate);

        Set<AlertNotification> alertNotifications = alertDefinition.getAlertNotifications();
        for (AlertNotification alertNotification : alertNotifications) {
            if (alertNotification instanceof SnmpNotification) {
                alertNotifications.remove(alertNotification);
            }
        }

        alertNotifications.add(snmpNotification);

        if (isAlertTemplate) {
            try {
                alertTemplateManager.updateAlertTemplate(subjectManager.getOverlord(), alertDefinition, true);
            } catch (InvalidAlertDefinitionException iade) {
                // can this ever really happen?  if it does, the logs will know about it
                LOG.error("Can not update alert template, invalid definition: " + alertDefinition);
            }
        }
    }
}