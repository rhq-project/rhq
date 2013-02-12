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

        //list of pagelists
        private List<PageList<FakeEntity>> pages = new ArrayList<PageList<FakeEntity>>();

        //total size
        private int totalSize;

        // the pageControl instance to use
        private PageControl pc;

        //
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

        CriteriaQuery<FakeEntity, FakeEntityCriteria> query =
            new CriteriaQuery<FakeEntity, FakeEntityCriteria>(criteria, queryExecutor);

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

        CriteriaQuery<FakeEntity, FakeEntityCriteria> query =
            new CriteriaQuery<FakeEntity, FakeEntityCriteria>(criteria, queryExecutor);

        List<FakeEntity> actual = new ArrayList<FakeEntity>();
        for (FakeEntity entity : query) {
            actual.add(entity);
        }

        assertEquals(actual, expected);
    }

    /** This is like executeQueryThatReturnsMultiplePagesOfResults(), creates more
     *  that two pages of ordered entries and iterates over them.  This is to test
     *  a nasty bug in CriteriaQuery where results beyond the first two pages were 
     *  not being parsed.
     */
    @Test
    public void executeQueryThatReturnsTotalPagesOfResults() {
        //create page control to browse entries 100 at a time and start at page 0
        int pageSize = 100;
        PageControl pc = new PageControl(0, pageSize);

        //Total size of result set is 500.
        int totalSize = 500;

        //Create list and populate with all entries.
        List<FakeEntity> total = new ArrayList<FakeEntity>();
        for (int i = 0; i < totalSize; i++) {
            total.add(new FakeEntity(i));
        }

        //build executor to parse a given list with using PageControl passed in
        FakeCriteriaQueryExecutor queryExecutor = new FakeCriteriaQueryExecutor(totalSize, pc);

        //add pages of results to simulate PageList results as returned by db queries
        //todo: spinder, modify to support fractional results below and add last page.
        int bucketCount = totalSize / pageSize;//number of full pages to list
        int start = 0;
        int end = pageSize;
        //add bucketCount pages of data to read from.
        for (int i = 0; i < bucketCount; i++) {
            //Ex. first two pages (0, 100), (100,200), etc. 
            queryExecutor.addPage(total.subList(start, end));
            start += pageSize;
            end += pageSize;
        }

        //build criteria and attach pageControl
        FakeEntityCriteria criteria = new FakeEntityCriteria();
        //DO NOT use criteria.setPageControl(pc) here as it causes ignore of pageNumber/pageSize
        criteria.setPaging(pc.getPageNumber(), pc.getPageSize());

        //?? So which pageControl has the right details? Criteria.pageControl? OR PageControl passed into the QueryExecutor.

        //Start off the initial query to page through the items in chunks defined by the pageControl instance
        CriteriaQuery<FakeEntity, FakeEntityCriteria> query = new CriteriaQuery<FakeEntity, FakeEntityCriteria>(
            criteria, queryExecutor);

        //Now iterate over the list and make sure that iteration happens in order as expected
        //monotonically increasing.
        int last = -1;
        for (FakeEntity entity : query) {
            //this fails with earlier bug in CriteriaQuery
            assertEquals(true, (last < entity.getId()));
            last = entity.getId();
        }
    }
}
