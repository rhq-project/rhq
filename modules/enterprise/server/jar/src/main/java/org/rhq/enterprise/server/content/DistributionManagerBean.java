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

package org.rhq.enterprise.server.content;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.DistributionType;
import org.rhq.core.domain.content.RepoDistribution;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;

/**
 * @author Pradeep Kilambi
 */
@Stateless
public class DistributionManagerBean implements DistributionManagerLocal, DistributionManagerRemote {

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @EJB
    private AgentManagerLocal agentManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private DistributionManagerLocal distributionManager;

    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Distribution createDistribution(Subject user, String kslabel, String basepath, DistributionType disttype)
        throws DistributionException {

        Distribution kstree = new Distribution(kslabel, basepath, disttype);
        System.out.println("NEW DISTRO CREATED" + kstree);
        validateDistTree(kstree);

        entityManager.persist(kstree);
        System.out.println("persisted" + kstree);

        return kstree;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteDistributionByRepo(Subject user, int repoId) {
        log.debug("User [" + user + "] is deleting distribution tree from repo [" + repoId + "]");

        entityManager.flush();
        entityManager.clear();

        entityManager.createNamedQuery(RepoDistribution.DELETE_BY_REPO_ID).setParameter("repoId", repoId)
            .executeUpdate();

        RepoDistribution kstree = entityManager.find(RepoDistribution.class, repoId);
        if (kstree != null) {
            entityManager.remove(kstree);
            log.debug("User [" + user + "] deleted kstree [" + kstree + "]");
        } else {
            log.debug("Repo ID [" + repoId + "] doesn't exist - nothing to delete");
        }

    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteDistributionByDistId(Subject user, int distId) {
        log.debug("User [" + user + "] is deleting distribution tree [" + distId + "]");

        entityManager.flush();
        entityManager.clear();

        entityManager.createNamedQuery(Distribution.QUERY_DELETE_BY_DIST_ID).setParameter("distid", distId)
            .executeUpdate();

        Distribution kstree = entityManager.find(Distribution.class, distId);
        if (kstree != null) {
            entityManager.remove(kstree);
            log.debug("User [" + user + "] deleted kstree [" + kstree + "]");
        } else {
            log.debug("Distribution tree ID [" + distId + "] doesn't exist - nothing to delete");
        }

    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void getDistributionBits(Subject user, String kslabel) {
        //TODO: Implement the ks tree bit downloads
    }

    @SuppressWarnings("unchecked")
    public Distribution getDistributionByLabel(String kslabel) {
        Query query = entityManager.createNamedQuery(Distribution.QUERY_FIND_BY_DIST_LABEL);

        query.setParameter("label", kslabel);
        List<Distribution> results = query.getResultList();

        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }

    }

    /**
     * Returns a kickstarttree from the database for a given basepath
     * @param basepath location on filesystem
     * @return kickstarttree object from db
     */
    @SuppressWarnings("unchecked")
    public Distribution getDistributionByPath(String basepath) {
        Query query = entityManager.createNamedQuery(Distribution.QUERY_FIND_BY_DIST_PATH);

        query.setParameter("base_path", basepath);
        List<Distribution> results = query.getResultList();

        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }

    }

    /**
     * validates a given kickstart tree object and throws a KickstartTreeException
     * @param kstree kickstart tree object
     * @throws DistributionException
     */
    public void validateDistTree(Distribution kstree) throws DistributionException {

        if (kstree.getLabel() == null || kstree.getLabel().trim().equals("")) {
            throw new DistributionException("A valid Distribution tree is required");
        }

        Distribution kstreeobj = getDistributionByLabel(kstree.getLabel());
        if (kstreeobj != null) {
            throw new DistributionException("There is already a kstree with the name of [" + kstree.getLabel() + "]");
        }

    }

}
