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

package org.rhq.server.metrics.migrator;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;

/**
 * @author Stefan Negrea
 */
public class ScrollableDataSource implements ExistingDataSource {

    private static final Log log = LogFactory.getLog(ExistingDataJPASource.class);

    private EntityManager entityManager;
    private String selectNativeQuery;

    private ScrollableResults results;
    private StatelessSession session;
    private int lastMigratedItemIndex;

    public ScrollableDataSource(EntityManager entityManager, String selectNativeQuery) {
        this.entityManager = entityManager;
        this.selectNativeQuery = selectNativeQuery;
    }

    @Override
    public List<Object[]> getData(int fromIndex, int maxResults) throws Exception {
        if (fromIndex != lastMigratedItemIndex + 1) {
            throw new Exception("Cursor error. " + fromIndex + " " + lastMigratedItemIndex);
        }

        if (log.isDebugEnabled()) {
            log.debug("Reading lines " + fromIndex + " to " + (fromIndex + maxResults));
        }

        List<Object[]> resultList = new ArrayList<Object[]>();

        int maxLimit = lastMigratedItemIndex + maxResults;

        while (results.next()) {
            resultList.add(results.get());

            if (++lastMigratedItemIndex == maxLimit) {
                break;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Read lines " + fromIndex + " to " + (fromIndex + maxResults));
        }

        return resultList;
    }

    @Override
    public void initialize() {
        if (session != null || results != null) {
            this.close();
        }

        session = ((Session) entityManager.getDelegate()).getSessionFactory().openStatelessSession();

        Query query = session.createSQLQuery(selectNativeQuery);
        query.setFetchSize(30000);
        query.setReadOnly(true);
        results = query.scroll(ScrollMode.FORWARD_ONLY);

        lastMigratedItemIndex = -1;
    }

    @Override
    public void close() {
        if (results != null) {
            results.close();
            results = null;
        }

        if (session != null) {
            session.close();
            session = null;
        }
    }
}
