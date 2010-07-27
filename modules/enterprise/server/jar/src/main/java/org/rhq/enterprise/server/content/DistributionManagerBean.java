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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.DistributionFile;
import org.rhq.core.domain.content.DistributionType;
import org.rhq.core.domain.content.RepoDistribution;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.RequiredPermission;

/**
 * @author Pradeep Kilambi
 */
@Stateless
public class DistributionManagerBean implements DistributionManagerLocal {

    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(this.getClass());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private DistributionManagerLocal distributionManager;

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Distribution createDistribution(Subject user, String kslabel, String basepath, DistributionType disttype)
        throws DistributionException {

        DistributionType loaded = distributionManager.getDistributionTypeByName(disttype.getName());
        if (loaded != null) {
            disttype = loaded;
        }

        Distribution kstree = new Distribution(kslabel, basepath, disttype);

        validateDistTree(kstree);
        entityManager.persist(kstree);

        return kstree;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void deleteDistributionMappingsForRepo(Subject user, int repoId) {
        log.debug("User [" + user + "] is removing distribution tree mapping from repository [" + repoId + "]");

        entityManager.createNamedQuery(RepoDistribution.DELETE_BY_REPO_ID).setParameter("repoId", repoId)
            .executeUpdate();
    }

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
     * Returns a DistributionType for given name
     * @param name name of distribution type
     * @return distribution type from db
     */
    @SuppressWarnings("unchecked")
    public DistributionType getDistributionTypeByName(String name) {
        Query query = entityManager.createNamedQuery(DistributionType.QUERY_FIND_BY_NAME);

        query.setParameter("name", name);
        List<DistributionType> results = query.getResultList();

        if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }

    }

    /**
     * Returns a list of available distribution files for requested distribution
     * @param distId
     * @return A list of Distributionfile objects associated to a given distribution
     */
    @SuppressWarnings("unchecked")
    public List<DistributionFile> getDistributionFilesByDistId(int distId) {
        Query query = entityManager.createNamedQuery(DistributionFile.SELECT_BY_DIST_ID);

        query.setParameter("distId", distId);
        List<DistributionFile> results = query.getResultList();

        if (results.size() > 0) {
            return results;
        } else {
            return null;
        }
    }

    /**
     * deletes list of available distribution files for requested distribution
     * @param distId
     */
    public void deleteDistributionFilesByDistId(Subject user, int distId) {
        log.debug("User [" + user + "] is deleting distribution file from distribution [" + distId + "]");

        entityManager.flush();
        entityManager.clear();

        Query querydel = entityManager.createNamedQuery(DistributionFile.SELECT_BY_DIST_ID);

        querydel.setParameter("distId", distId);

        querydel.executeUpdate();

        DistributionFile distFile = entityManager.find(DistributionFile.class, distId);
        if (distFile != null) {
            entityManager.remove(distFile);
            log.debug("User [" + user + "] deleted distribution file [" + distFile + "]");
        } else {
            log.debug("Distribution file [" + distFile + "] doesn't exist - nothing to delete");
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
