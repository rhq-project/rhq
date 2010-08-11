/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.dashboard;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.dashboard.Dashboard;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerBean;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;

/**
 *
 * @author Greg Hinkle
 */
@Stateless
public class DashboardManagerBean implements DashboardManagerLocal, DashboardManagerRemote{

    @SuppressWarnings("unused")
    private final Log log = LogFactory.getLog(SubjectManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;


    public List<Dashboard> findDashboardsForSubject(Subject subject) {
        Query query = entityManager.createQuery("SELECT d FROM Dashboard d WHERE d.owner.id = :subjectId");

        query.setParameter("subjectId", subject.getId());

        return query.getResultList();

    }

    public List<Dashboard> findSharedDashboards(Subject subject) {
        Query query = entityManager.createQuery("SELECT d FROM Dashboard d WHERE d.shared = true");

        return query.getResultList();
    }

    public Dashboard storeDashboard(Subject subject, Dashboard dashboard) {
        Dashboard d = null;
        if ((d = entityManager.find(Dashboard.class, dashboard.getId())) == null) {
            dashboard.setOwner(subject);
            entityManager.persist(dashboard);
            return dashboard;
        } else {
            if (!authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_SETTINGS) && d.getOwner().getId() != subject.getId()) {
                throw new PermissionException("You may only alter dashboards you own.");
            }
            return entityManager.merge(dashboard);
        }
    }

    public void removeDashboard(Subject subject, int dashboardId) {

        Dashboard toDelete = entityManager.find(Dashboard.class,dashboardId);

        if (!authorizationManager.hasGlobalPermission(subject, Permission.MANAGE_SETTINGS) && toDelete.getOwner().getId() != subject.getId()) {
            throw new PermissionException("You may only delete dashboards you own.");
        }

        entityManager.remove(toDelete);
    }
}
