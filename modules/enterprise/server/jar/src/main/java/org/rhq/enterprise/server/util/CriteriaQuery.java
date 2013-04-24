/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */
package org.rhq.enterprise.server.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.rhq.core.domain.criteria.BaseCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;

/** This class provides a way to make PageList results easily iterable with 'for each','while',etc. loops
 *  and importantly automatically handles iteration through all PageControl results.  This 
 *  means that with a CriteriaQuery instance once can do:
 *  
 *   for (Resource entity : query) { 
 * 
 * and automatically page through all of the results in PageControl.getPageSize(def. 200) chunks.
 * 
 * @author John Sanda
 * @author Simeon Pinder
 *
 * @param <T> The return type included by the PageList. 
 * @param <C> The Criteria subclass used to generate/execute the query.
 */
public class CriteriaQuery<T, C extends BaseCriteria> implements Iterable<T> {

    //Criteria instance used by Executor to page through results
    private C criteria;

    //Executor
    private CriteriaQueryExecutor<T, C> queryExecutor;

    /**
     * It is important that the <code>criteria</code> includes sorting.  If not then paging is nonsensical as the DB
     * provides no guarantee of ordering.  If no sort is specified, an implicit sort on ID is added.
     * 
     * @param criteria The criteria applied to each execution of the fetch. If no sort is specified, an implicit sort on
     * ID is added.
     * @param queryExecutor
     */
    public CriteriaQuery(C criteria, CriteriaQueryExecutor<T, C> queryExecutor) {
        this.criteria = criteria;
        this.queryExecutor = queryExecutor;

        // make sure we have at least a default sort, otherwise chunking doesn't work
        PageControl pageControlOverrides = this.criteria.getPageControlOverrides();
        if (null != pageControlOverrides) {
            if (pageControlOverrides.getOrderingFields().isEmpty()) {
                pageControlOverrides.addDefaultOrderingField("id");
            }

        } else if (this.criteria.getOrderingFieldNames().isEmpty()) {
            this.criteria.addSortId(PageOrdering.ASC);
        }
    }

    /** Returns iterator for a single page of results as defined by
     * i)the Criteria instance
     * ii)the paging details applied to the Criteria instance
     */
    @Override
    public Iterator<T> iterator() {
        return new QueryResultsIterator(executeQuery());
    }

    private PageList<T> executeQuery() {
        return queryExecutor.execute(criteria);
    }

    //Defines the iterator that:
    //    i)creates page sized chunks results
    //    ii)at the end of each pageList, moves the iterator to next page and continues iteration
    //
    //NOTE: Assumes criteria page iteration starts with page 0. Will continue to iterate over N members.
    protected class QueryResultsIterator implements Iterator<T> {
        private int count;

        private PageList<T> currentPage;

        private Iterator<T> iterator;

        private T deletable = null;

        private ArrayList<T> forDeletion = new ArrayList<T>();

        /**The first pageList returned by the criteria instance is where iteration begins.
         * @param firstPage
         */
        public QueryResultsIterator(PageList<T> firstPage) {
            currentPage = firstPage;
            iterator = currentPage.iterator();
            count = firstPage == null || firstPage.getPageControl() == null ? 0 : firstPage.getPageControl().getStartRow();
        }

        @Override
        public boolean hasNext() {
            return count < currentPage.getTotalSize();
        }

        @Override
        public T next() {
            //define logic for the end of a pagelist to move the iterator onto next page
            if (!iterator.hasNext()) {
                if (count == currentPage.getTotalSize()) {
                    throw new NoSuchElementException();
                }

                deletable = null; // reset deletable.

                //remove all flagged instances of T
                if (!forDeletion.isEmpty()) {
                    currentPage.removeAll(forDeletion);
                    forDeletion.clear();
                }

                // advance the page. Although strange to be using a page control override in conjunction with
                // CriteriaQuery, nonetheless make sure we advance it if it exists, because the normal setPaging is
                // ignored when their is an overrides.
                PageControl pcCurrent = currentPage.getPageControl();
                PageControl pcOverrides = criteria.getPageControlOverrides();

                if (null != pcOverrides) {
                    pcOverrides.setPageNumber(pcOverrides.getPageNumber() + 1);
                } else {
                    criteria.setPaging(pcCurrent.getPageNumber() + 1, pcCurrent.getPageSize());
                }

                //help out the GC.
                currentPage.clear();

                currentPage = queryExecutor.execute(criteria);
                iterator = currentPage.iterator();
            }

            T next = iterator.next();
            deletable = next;
            count++;
            return next;
        }

        @Override
        public void remove() {
            if (deletable != null) {
                forDeletion.add(deletable);
                deletable = null;
            } else {
                throw new IllegalStateException(
                    "Not allowed to call remove() without calling next() just before this call.");
            }
        }
    }
}
