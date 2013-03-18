/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

/**
 * @author Thomas Segismont
 */
public class SavedSearchManagerBeanTest extends AbstractEJB3Test {

    private SavedSearchManagerLocal savedSearchManager;

    @Override
    protected void beforeMethod() throws Exception {
        savedSearchManager = LookupUtil.getSavedSearchManager();
    }

    @Test
    public void testCRUD() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Subject subject = SessionTestHelper.createNewSubject(em, "fake subject");
                int entityId = savedSearchManager.createSavedSearch(subject, new SavedSearch(SearchSubsystem.GROUP,
                    "fake saved search", "pipo", subject));
                SavedSearch entity = savedSearchManager.getSavedSearchById(subject, entityId);
                assertNotNull(entity);
                SavedSearch modifiedEntity = new SavedSearch(SearchSubsystem.RESOURCE, "fake saved search modified",
                    "molo", subject);
                modifiedEntity.setId(entityId);
                boolean updateSuccess = savedSearchManager.updateSavedSearch(subject, modifiedEntity);
                assertTrue("Update save searched failed", updateSuccess);
                savedSearchManager.deleteSavedSearch(subject, entityId);
                assertNull(em.find(SavedSearch.class, entityId));
            }
        });
    }

    @Test
    public void testgetSavedSearchByIdPermission() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                Subject subject1 = SessionTestHelper.createNewSubject(em, "fake subject 1");
                SavedSearch savedSearch = new SavedSearch(SearchSubsystem.GROUP, "fake saved search", "pipo", subject1);
                int entityId = savedSearchManager.createSavedSearch(subject1, savedSearch);
                Subject subject2 = SessionTestHelper.createNewSubject(em, "fake subject 2");
                try {
                    savedSearchManager.getSavedSearchById(subject2, entityId);
                    fail("Expected " + PermissionException.class.getSimpleName());
                } catch (PermissionException e) {
                    assertTrue(e.getMessage().contains("view"));
                }
            }
        });
    }

    @Test
    public void testCreateSavedSearchPermission() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                final Subject subject1 = SessionTestHelper.createNewSubject(em, "fake subject 1");
                final Subject subject2 = SessionTestHelper.createNewSubject(em, "fake subject 2");
                assertManipulatePermissionExceptionThrown(new Runnable() {
                    @Override
                    public void run() {
                        savedSearchManager.createSavedSearch(subject2, new SavedSearch(SearchSubsystem.GROUP,
                            "fake saved search 2", "molo", subject1));
                    }
                });
            }
        });
    }

    @Test
    public void testDeleteSavedSearchPermission() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                final Subject subject1 = SessionTestHelper.createNewSubject(em, "fake subject 1");
                final int entityId = savedSearchManager.createSavedSearch(subject1, new SavedSearch(
                    SearchSubsystem.GROUP, "fake saved search", "pipo", subject1));
                final Subject subject2 = SessionTestHelper.createNewSubject(em, "fake subject 2");
                assertManipulatePermissionExceptionThrown(new Runnable() {
                    @Override
                    public void run() {
                        savedSearchManager.deleteSavedSearch(subject2, entityId);
                    }
                });
            }
        });
    }

    @Test
    public void testUpdateSavedSearchPermission() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                final Subject subject1 = SessionTestHelper.createNewSubject(em, "fake subject 1");
                final int entityId = savedSearchManager.createSavedSearch(subject1, new SavedSearch(
                    SearchSubsystem.GROUP, "fake saved search", "pipo", subject1));
                final Subject subject2 = SessionTestHelper.createNewSubject(em, "fake subject 2");
                assertManipulatePermissionExceptionThrown(new Runnable() {
                    @Override
                    public void run() {
                        SavedSearch modifiedEntity = new SavedSearch(SearchSubsystem.GROUP, "fake saved search 2",
                            "molo", subject1);
                        modifiedEntity.setId(entityId);
                        savedSearchManager.updateSavedSearch(subject2, modifiedEntity);
                    }
                });
            }
        });
    }

    private void assertManipulatePermissionExceptionThrown(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected " + PermissionException.class.getSimpleName());
        } catch (PermissionException e) {
            assertTrue(e.getMessage().contains("manipulate"));
        }
    }

    @Test
    public void testFindSavedSearchesByCriteria() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {

                Subject subject1 = SessionTestHelper.createNewSubject(em, "fake subject 1");
                Subject subject2 = SessionTestHelper.createNewSubject(em, "fake subject 2");
                Role inventoryManagerRole = SessionTestHelper.createNewRoleForSubject(em, subject2,
                    "inventory manager role", Permission.MANAGE_INVENTORY);

                for (int i = 0; i < 1000; i++) {
                    String iStr = String.valueOf(i);
                    savedSearchManager.createSavedSearch(subject1, new SavedSearch(SearchSubsystem.GROUP,
                        "fake saved search " + iStr, "pipo " + iStr, subject1));
                    savedSearchManager.createSavedSearch(subject2, new SavedSearch(SearchSubsystem.GROUP,
                        "fake saved search " + iStr, "molo " + iStr, subject2));
                }

                SavedSearchCriteria criteria = new SavedSearchCriteria();
                criteria.addFilterName("fake saved search");
                criteria.clearPaging();

                PageList<SavedSearch> foundEntities = savedSearchManager
                    .findSavedSearchesByCriteria(subject2, criteria);
                assertTrue("User with manage inventory should see all saved searches", 2000 == foundEntities.size());

                foundEntities = savedSearchManager.findSavedSearchesByCriteria(subject1, criteria);
                assertTrue("User without manage inventory should see only its own saved searches",
                    1000 == foundEntities.size());

            }
        });
    }
}
