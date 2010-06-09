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
package org.rhq.enterprise.server.search;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * This bean provides functionality to CRUD saved search patterns.
 *
 * @author Joseoh Marques
 */
@Stateless
public class SavedSearchManagerBean implements SavedSearchManagerLocal /* local already implements remote interface */{

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    /**
     * @see SavedSearchManagerRemote#createSavedSearch(Subject, SavedSearch)
     */
    public int createSavedSearch(Subject subject, SavedSearch savedSearch) {
        validateManipulatePermission(subject, savedSearch);
        entityManager.persist(savedSearch);
        return savedSearch.getId();
    }

    /**
     * @see SavedSearchManagerRemote#updateSavedSearch(Subject, SavedSearch)
     */
    public void updateSavedSearch(Subject subject, SavedSearch savedSearch) {
        // this needs to prevent certains types of updates, be more sophisticated, etc
        validateManipulatePermission(subject, savedSearch);
        entityManager.merge(savedSearch);
        return;
    }

    /**
     * @see SavedSearchManagerRemote#deleteSavedSearch(Subject, int)
     */
    public void deleteSavedSearch(Subject subject, int savedSearchId) {
        SavedSearch savedSearch = entityManager.find(SavedSearch.class, savedSearchId);
        validateManipulatePermission(subject, savedSearch);
        entityManager.remove(savedSearch);
    }

    /**
     * @see SavedSearchManagerRemote#getSavedSearchById(Subject, int)
     */
    public SavedSearch getSavedSearchById(Subject subject, int savedSearchId) {
        SavedSearch savedSearch = entityManager.find(SavedSearch.class, savedSearchId);
        validateReadPermission(subject, savedSearch);
        return savedSearch;
    }

    public PageList<SavedSearch> findSavedSearchesByCriteria(Subject subject, SavedSearchCriteria criteria) {
        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        if (!authorizationManager.isInventoryManager(subject)) {
            generator.setAuthorizationCustomConditionFragment("(subject.id=" + subject.getId() + " OR global=true)");
        }

        CriteriaQueryRunner<SavedSearch> queryRunner = new CriteriaQueryRunner<SavedSearch>(criteria, generator,
            entityManager);
        return queryRunner.execute();
    }

    private void validateManipulatePermission(Subject subject, SavedSearch savedSearch) {
        if (savedSearch.isGlobal()) {
            if (!authorizationManager.isInventoryManager(subject)) {
                throw new PermissionException("Only inventory managers can manipulate global saved searches");
            }
            // note: inventory managers can modify any saved search pattern, not just their own
        } else {
            if (subject.getId() != savedSearch.getSubjectId() && !authorizationManager.isInventoryManager(subject)) {
                throw new PermissionException("Users without inventory manager permission "
                    + "can only manipulate their own saved searches");
            }
        }
    }

    private void validateReadPermission(Subject subject, SavedSearch savedSearch) {
        if (!savedSearch.isGlobal()) {
            if (subject.getId() != savedSearch.getSubjectId() && !authorizationManager.isInventoryManager(subject)) {
                throw new PermissionException("Users without inventory manager permission "
                    + "can only view their own or global saved saved searches");
            }
        }
    }
}
