/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.resource.test;

import static org.rhq.core.domain.util.PageOrdering.ASC;

import java.util.Arrays;

import javax.ejb.EJBException;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;

/**
 * @author Thomas Segismont
 */
public class GroupMemberCandidateResourcesTest extends AbstractEJB3Test {

    private static final String GROUP_MEMBER_CANDIDATE_NAME_PREFIX = GroupMemberCandidateResourcesTest.class
        .getSimpleName() + "-";

    private Subject overlord;

    private ResourceManagerLocal resourceManager;

    @Override
    protected void beforeMethod() throws Exception {
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        overlord = subjectManager.getOverlord();
        resourceManager = LookupUtil.getResourceManager();
    }

    @Test
    public void testNoCandidates() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                ResourceCriteria criteria = new ResourceCriteria();
                criteria.setCaseSensitive(false);
                criteria.addFilterName(GROUP_MEMBER_CANDIDATE_NAME_PREFIX);

                PageList<Resource> groupMemberCandidateResources = resourceManager.findGroupMemberCandidateResources(
                    overlord, criteria, new int[0]);

                assertEquals(0, groupMemberCandidateResources.getTotalSize());
                assertEquals(0, groupMemberCandidateResources.size());
            }
        });
    }

    @Test
    public void testNoCandidatesWithExclusions() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                ResourceCriteria criteria = new ResourceCriteria();
                criteria.setCaseSensitive(false);
                criteria.addFilterName(GROUP_MEMBER_CANDIDATE_NAME_PREFIX);
                int[] alreadySelectedResourceIds = new int[100];
                for (int i = 0; i < alreadySelectedResourceIds.length; i++) {
                    alreadySelectedResourceIds[i] = i;
                }

                PageList<Resource> groupMemberCandidateResources = resourceManager.findGroupMemberCandidateResources(
                    overlord, criteria, alreadySelectedResourceIds);

                assertEquals(0, groupMemberCandidateResources.getTotalSize());
                assertEquals(0, groupMemberCandidateResources.size());
            }
        });
    }

    @Test
    public void testUnlimitedPageControl() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                ResourceCriteria criteria = new ResourceCriteria();
                criteria.setPageControl(PageControl.getUnlimitedInstance());

                try {
                    PageList<Resource> groupMemberCandidateResources = resourceManager
                        .findGroupMemberCandidateResources(overlord, criteria, new int[0]);
                    fail("findGroupMemberCandidateResources should throw UnsupportedOperationException");
                } catch (EJBException e) {
                    assertTrue(e.getCausedByException() instanceof UnsupportedOperationException);
                }
            }
        });
    }

    @Test
    public void testPageControl() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                int resourcesCount = 100;
                createTestResources(resourcesCount);
                ResourceCriteria criteria = new ResourceCriteria();
                criteria.setCaseSensitive(false);
                criteria.addFilterName(GROUP_MEMBER_CANDIDATE_NAME_PREFIX);
                criteria.addSortName(ASC);
                int pageNumber = 5;
                int pageSize = 7;
                criteria.setPaging(pageNumber, pageSize);

                PageList<Resource> groupMemberCandidateResources = resourceManager.findGroupMemberCandidateResources(
                    overlord, criteria, new int[0]);

                assertEquals(resourcesCount, groupMemberCandidateResources.getTotalSize());
                assertEquals(pageSize, groupMemberCandidateResources.size());
                for (Resource groupMemberCandidateResource : groupMemberCandidateResources) {
                    assertTrue(groupMemberCandidateResource.getName().startsWith(GROUP_MEMBER_CANDIDATE_NAME_PREFIX));
                }
                assertEquals(pageNumber, groupMemberCandidateResources.getPageControl().getPageNumber());
                assertEquals(pageSize, groupMemberCandidateResources.getPageControl().getPageSize());
            }
        });
    }

    @Test
    public void testPageControlWithExclusions() {
        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                int resourcesCount = 100;
                int[] resourcesIds = createTestResources(resourcesCount);
                ResourceCriteria criteria = new ResourceCriteria();
                criteria.setCaseSensitive(false);
                criteria.addFilterName(GROUP_MEMBER_CANDIDATE_NAME_PREFIX);
                criteria.addSortName(ASC);
                int pageNumber = 5;
                int pageSize = 7;
                criteria.setPaging(pageNumber, pageSize);
                int[] alreadySelectedResourceIds = Arrays.copyOfRange(resourcesIds, 13, 59);

                PageList<Resource> groupMemberCandidateResources = resourceManager.findGroupMemberCandidateResources(
                    overlord, criteria, alreadySelectedResourceIds);

                assertEquals(resourcesCount, groupMemberCandidateResources.getTotalSize());
                assertEquals(pageSize, groupMemberCandidateResources.size());
                for (Resource groupMemberCandidateResource : groupMemberCandidateResources) {
                    assertTrue(groupMemberCandidateResource.getName().startsWith(GROUP_MEMBER_CANDIDATE_NAME_PREFIX));
                }
                assertEquals(pageNumber, groupMemberCandidateResources.getPageControl().getPageNumber());
                assertEquals(pageSize, groupMemberCandidateResources.getPageControl().getPageSize());
            }
        });
    }

    private int[] createTestResources(int resourcesCount) {
        int[] resourcesIds = new int[resourcesCount];
        ResourceType testResourceType = SessionTestHelper.createNewResourceType(em);
        for (int i = 0; i < resourcesCount; i++) {
            resourcesIds[i] = SessionTestHelper.createNewResource(em, GROUP_MEMBER_CANDIDATE_NAME_PREFIX + i,
                testResourceType).getId();
        }
        return resourcesIds;
    }

}
