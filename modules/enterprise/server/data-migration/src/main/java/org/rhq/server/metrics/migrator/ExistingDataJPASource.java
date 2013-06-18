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

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Thomas Segismont
 */
public class ExistingDataJPASource implements ExistingDataSource {

    private static final Log log = LogFactory.getLog(ExistingDataJPASource.class);

    private EntityManager entityManager;
    private String selectNativeQuery;

    public ExistingDataJPASource(EntityManager entityManager, String selectNativeQuery) {
        this.entityManager = entityManager;
        this.selectNativeQuery = selectNativeQuery;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Object[]> getData(int fromIndex, int maxResults) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Reading lines " + fromIndex + " to " + (fromIndex + maxResults));
        }

        return (List<Object[]>) entityManager.createNativeQuery(selectNativeQuery).setFirstResult(fromIndex)
            .setMaxResults(maxResults).getResultList();
    }

    @Override
    public void initialize() {
        //nothing to do since it just implements a simple query with limits
    }

    @Override
    public void close() {
        //nothing to do since it just implements a simple query with limits
    }
}
