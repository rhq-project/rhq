/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.util;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.core.domain.util.PersistenceUtility;

/**
 * A utility to track down how many round trips hibernate may be doing for a given set of code.
 * This just tracks the difference in raw connect counts and can be thrown off by connections
 * made in other threads
 *
 * @author Greg Hinkle
 */
public class StatisticsUtility {

    private Log log = LogFactory.getLog(StatisticsUtility.class);
    private SessionFactory sessionFactory;
    long connectionsAtStart;
    long beginTime;

    public StatisticsUtility() {


        this.sessionFactory = PersistenceUtility.getHibernateSession(LookupUtil.getEntityManager())
                .getSessionFactory();
        Statistics stats = sessionFactory.getStatistics();

        connectionsAtStart = stats.getConnectCount();
        beginTime = System.currentTimeMillis();
    }

    public void logStats() {
        Statistics stats = sessionFactory.getStatistics();

        log.info("HibernateStats: " + (stats.getConnectCount() - connectionsAtStart) +
                " connections made in " + (System.currentTimeMillis() - beginTime) + "ms");
    }
}
