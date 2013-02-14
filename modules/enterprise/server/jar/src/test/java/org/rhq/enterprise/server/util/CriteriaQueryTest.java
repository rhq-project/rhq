/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.server.util;

import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

public class CriteriaQueryTest {

    private static class FakeEntity {
        private int id;

        public FakeEntity(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    private static class FakeEntityCriteria extends Criteria {
        @Override
        public Class<?> getPersistentClass() {
            return FakeEntity.class;
        }
    }

    private static class FakeCriteriaQueryExecutor implements CriteriaQueryExecutor<FakeEntity, FakeEntityCriteria> {

        private List<PageList<FakeEntity>> pages = new ArrayList<PageList<FakeEntity>>();

        private int totalSize;

        private PageControl pc;

        public FakeCriteriaQueryExecutor(int totalSize, PageControl pc) {
            this.totalSize = totalSize;
            this.pc = pc;
        }

        public void addPage(List<FakeEntity> entities) {
            pages.add(new PageList<FakeEntity>(entities, totalSize, pc));
        }

        @Override
        public PageList<FakeEntity> execute(FakeEntityCriteria criteria) {
            return pages.get(criteria.getPageNumber());
        }
    }

    @Test
    public void executeQueryThatReturnsASinglePageOfResults() {
        List<FakeEntity> expected = asList(new FakeEntity(1), new FakeEntity(2));

        FakeCriteriaQueryExecutor queryExecutor = new FakeCriteriaQueryExecutor(2, PageControl.getUnlimitedInstance());
        queryExecutor.addPage(expected);

        FakeEntityCriteria criteria = new FakeEntityCriteria();

        CriteriaQuery<FakeEntity, FakeEntityCriteria> query = new CriteriaQuery<FakeEntity, FakeEntityCriteria>(
            criteria, queryExecutor);

        List<FakeEntity> actual = new ArrayList<FakeEntity>();
        for (FakeEntity entity : query) {
            actual.add(entity);
        }

        assertEquals(actual, expected, "Failed to iterate over query results with a single page");
    }

    @Test
    public void executeQueryThatReturnsMultiplePagesOfResults() {
        PageControl pc = new PageControl(0, 2);

        List<FakeEntity> expected = asList(new FakeEntity(1), new FakeEntity(2), new FakeEntity(3), new FakeEntity(4));

        FakeCriteriaQueryExecutor queryExecutor = new FakeCriteriaQueryExecutor(4, pc);
        queryExecutor.addPage(expected.subList(0, 2));
        queryExecutor.addPage(expected.subList(2, 4));

        FakeEntityCriteria criteria = new FakeEntityCriteria();
        criteria.setPageControl(pc);

        CriteriaQuery<FakeEntity, FakeEntityCriteria> query = new CriteriaQuery<FakeEntity, FakeEntityCriteria>(
            criteria, queryExecutor);

        List<FakeEntity> actual = new ArrayList<FakeEntity>();
        for (FakeEntity entity : query) {
            actual.add(entity);
        }

        assertEquals(actual, expected);
    }

    @Test
    public void singleResultTest() {
        // This test doesn't really fit here but I;m adding it for convenience
        List<FakeEntity> result = null;

        try {
            FakeEntityCriteria.getSingleResult(result);
            assert false : "Should have thrown Runtime Exception";

        } catch (RuntimeException e) {
            assert e.getMessage().contains("NoResultException");
        }

        result = new ArrayList<FakeEntity>(2);

        try {
            FakeEntityCriteria.getSingleResult(result);
            assert false : "Should have thrown Runtime Exception";

        } catch (RuntimeException e) {
            assert e.getMessage().contains("NoResultException");
        }

        result.add(new FakeEntity(1));

        try {
            FakeEntity r = FakeEntityCriteria.getSingleResult(result);
            assert r.getId() == 1 : "Should have retuned expected entity but returned: " + r;

        } catch (Throwable t) {
            assert false : "Should have returned single result";
        }

        result.add(new FakeEntity(2));

        try {
            FakeEntityCriteria.getSingleResult(result);
            assert false : "Should have thrown Runtime Exception";

        } catch (RuntimeException e) {
            assert e.getMessage().contains("NonUniqueResultException");
        }
    }

}
