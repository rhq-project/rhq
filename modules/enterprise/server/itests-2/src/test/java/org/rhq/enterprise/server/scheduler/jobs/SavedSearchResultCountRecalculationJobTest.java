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

package org.rhq.enterprise.server.scheduler.jobs;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.search.SavedSearchManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.test.TransactionCallbackReturnable;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.ResourceTreeHelper;
import org.rhq.enterprise.server.util.SessionTestHelper;

/**
 * @author Thomas Segismont
 */
public class SavedSearchResultCountRecalculationJobTest extends AbstractEJB3Test {

    private SavedSearchManagerLocal savedSearchManager;

    private SubjectManagerLocal subjectManager;

    private RoleManagerLocal roleManager;

    private ResourceGroupManagerLocal resourceGroupManager;

    private SchedulerLocal scheduler;

    private TestData testData;

    @Override
    protected void beforeMethod() throws Exception {
        savedSearchManager = LookupUtil.getSavedSearchManager();
        subjectManager = LookupUtil.getSubjectManager();
        roleManager = LookupUtil.getRoleManager();
        resourceGroupManager = LookupUtil.getResourceGroupManager();
        scheduler = LookupUtil.getSchedulerBean();
        createTestData();
        prepareScheduler();
    }

    private void createTestData() {
        testData = executeInTransaction(false, new TransactionCallbackReturnable<TestData>() {
            @Override
            public TestData execute() throws Exception {
                Subject searchesOwner = SessionTestHelper.createNewSubject(em, "fake subject");
                Role searchesOwnerRole = SessionTestHelper.createNewRoleForSubject(em, searchesOwner, "fake role");
                ResourceType resourceType = SessionTestHelper.createNewResourceType(em);
                Set<Resource> resources = new HashSet<Resource>();
                Set<ResourceGroup> resourceGroups = new HashSet<ResourceGroup>();
                Set<SavedSearch> savedSearches = new HashSet<SavedSearch>();
                for (int i = 0; i < 50; i++) {
                    String iStr = i > 9 ? String.valueOf(i) : "0" + String.valueOf(i);
                    if (i < 25) {
                        resources.add(SessionTestHelper.createNewResource(em, "resource-" + iStr, resourceType));
                        SavedSearch savedSearch = new SavedSearch(SearchSubsystem.RESOURCE, "search-" + iStr,
                            "resource-" + iStr, searchesOwner);
                        savedSearchManager.createSavedSearch(searchesOwner, savedSearch);
                        savedSearches.add(savedSearch);
                    } else {
                        resourceGroups.add(SessionTestHelper.createNewCompatibleGroupForRole(em, searchesOwnerRole,
                            "group-" + iStr, resourceType));
                        SavedSearch savedSearch = new SavedSearch(SearchSubsystem.GROUP, "search-" + iStr, "group-"
                            + iStr, searchesOwner);
                        savedSearchManager.createSavedSearch(searchesOwner, savedSearch);
                        savedSearches.add(savedSearch);
                    }
                }
                return new TestData(searchesOwner, searchesOwnerRole, resourceType, resources, resourceGroups,
                    savedSearches);
            }
        });
    }

    @Override
    protected void afterMethod() throws Exception {
        try {
            deleteTestData();
        } finally {
            unprepareScheduler();
        }
    }

    private void deleteTestData() {
        if (testData != null) {
            final TestData testDataToDelete = testData;
            testData = null;
            executeInTransaction(false, new TransactionCallback() {
                @Override
                public void execute() throws Exception {
                    for (SavedSearch savedSearch : testDataToDelete.getSavedSearches()) {
                        savedSearchManager.deleteSavedSearch(savedSearch.getSubject(), savedSearch.getId());
                    }
                    for (ResourceGroup resourceGroup : testDataToDelete.getResourceGroups()) {
                        resourceGroupManager.deleteResourceGroup(subjectManager.getOverlord(), resourceGroup.getId());
                    }
                    for (Resource resource : testDataToDelete.getResources()) {
                        ResourceTreeHelper.deleteResource(em, em.find(Resource.class, resource.getId()));
                    }
                    em.remove(em.find(ResourceType.class, testDataToDelete.getResourceType().getId()));
                    subjectManager.deleteSubjects(subjectManager.getOverlord(), new int[] { testDataToDelete
                        .getSearchesOwner().getId() });
                    roleManager.deleteRoles(subjectManager.getOverlord(), new int[] { testDataToDelete
                        .getSearchesOwnerRole().getId() });
                }
            });

        }
    }

    @Test
    public void testSavedSearchResultCountRecalculation() throws Exception {
        runJobSynchronously();
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                SavedSearchCriteria criteria = new SavedSearchCriteria();
                criteria.clearPaging();
                criteria.addFilterSubjectId(testData.getSearchesOwner().getId());
                PageList<SavedSearch> foundSavedSearches = savedSearchManager.findSavedSearchesByCriteria(
                    testData.getSearchesOwner(), criteria);
                assertEquals(testData.getSavedSearches().size(), foundSavedSearches.size());
                for (SavedSearch savedSearch : foundSavedSearches) {
                    assertTrue(savedSearch.toString() + " should have been computed",
                        savedSearch.getLastComputeTime() > 0);
                    assertEquals(Long.valueOf(1), savedSearch.getResultCount());
                }
            }
        });
    }

    private void runJobSynchronously() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final String jobName = SavedSearchResultCountRecalculationJob.class.getSimpleName();
        final String jobGroup = SavedSearchResultCountRecalculationJobTest.class.getSimpleName();
        scheduler.addGlobalJobListener(new JobListener() {
            @Override
            public String getName() {
                return SavedSearchResultCountRecalculationJobTest.class.getSimpleName();
            }

            @Override
            public void jobToBeExecuted(JobExecutionContext context) {
            }

            @Override
            public void jobExecutionVetoed(JobExecutionContext context) {
            }

            @Override
            public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
                if (context.getJobDetail().getGroup().equals(jobGroup)) {
                    countDownLatch.countDown();
                }
            }
        });
        scheduler.addJob(new JobDetail(jobName, jobGroup, SavedSearchResultCountRecalculationJob.class), true);
        scheduler.triggerJob(jobName, jobGroup);
        try {
            countDownLatch.await(60, TimeUnit.SECONDS);
        } finally {
            scheduler.deleteJob(jobName, jobGroup);
        }
    }

    private static final class TestData {

        private final Subject searchesOwner;

        private final Role searchesOwnerRole;

        private final ResourceType resourceType;

        private final Set<Resource> resources;

        private final Set<ResourceGroup> resourceGroups;

        private final Set<SavedSearch> savedSearches;

        private TestData(Subject searchesOwner, Role searchesOwnerRole, ResourceType resourceType,
            Set<Resource> resources, Set<ResourceGroup> resourceGroups, Set<SavedSearch> savedSearches) {
            this.searchesOwner = searchesOwner;
            this.searchesOwnerRole = searchesOwnerRole;
            this.resourceType = resourceType;
            this.resources = resources;
            this.resourceGroups = resourceGroups;
            this.savedSearches = savedSearches;
        }

        public Subject getSearchesOwner() {
            return searchesOwner;
        }

        public Role getSearchesOwnerRole() {
            return searchesOwnerRole;
        }

        public ResourceType getResourceType() {
            return resourceType;
        }

        public Set<Resource> getResources() {
            return resources;
        }

        public Set<ResourceGroup> getResourceGroups() {
            return resourceGroups;
        }

        public Set<SavedSearch> getSavedSearches() {
            return savedSearches;
        }
    }

}
