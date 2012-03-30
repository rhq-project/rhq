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
import java.util.NoSuchElementException;

import org.rhq.core.domain.criteria.BaseCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

public class CriteriaQuery<T, C extends BaseCriteria> implements Iterable<T> {

    private C criteria;

    private CriteriaQueryExecutor<T, C> queryExecutor;

    public CriteriaQuery(C criteria, CriteriaQueryExecutor<T, C> queryExecutor) {
        this.criteria = criteria;
        this.queryExecutor = queryExecutor;
    }

    @Override
    public Iterator<T> iterator() {
        return new QueryResultsIterator(executeQuery());
    }

    private PageList<T> executeQuery() {
        return queryExecutor.execute(criteria);
    }

    private class QueryResultsIterator implements Iterator<T> {
        private int count;

        private PageList<T> currentPage;

        private Iterator<T> iterator;

        public QueryResultsIterator(PageList<T> firstPage) {
            currentPage = firstPage;
            iterator = currentPage.iterator();
        }

        @Override
        public boolean hasNext() {
            return count < currentPage.getTotalSize();
        }

        @Override
        public T next() {
            if (!iterator.hasNext()) {
                if (count == currentPage.getTotalSize()) {
                    throw new NoSuchElementException();
                }

                PageControl pc = currentPage.getPageControl();
                criteria.setPaging(pc.getPageNumber() + 1, pc.getPageSize());
                currentPage = queryExecutor.execute(criteria);
                iterator = currentPage.iterator();
            }

            T next = iterator.next();
            count++;
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("This iterator does not support removal.");
        }
    }
}
