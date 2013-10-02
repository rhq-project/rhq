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

import java.util.Iterator;

import org.rhq.core.domain.criteria.BaseCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

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
        private PageList<T> currentPage;

        private Iterator<T> iterator;

        private boolean reachedEnd;

        /**The first pageList returned by the criteria instance is where iteration begins.
         * @param firstPage
         */
        public QueryResultsIterator(PageList<T> firstPage) {
            currentPage = firstPage;
            iterator = currentPage.iterator();
        }

        @Override
        public boolean hasNext() {
            if (!iterator.hasNext() && !reachedEnd) {
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
                reachedEnd = !iterator.hasNext(); //if we got an empty collection as a result for obtaining the next page
                                                  //we can be pretty sure we're past the number of available results
            }

            return iterator.hasNext();
        }

        @Override
        public T next() {
            return iterator.next();
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }
}
