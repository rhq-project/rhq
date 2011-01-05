/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support;

import java.util.Collections;

import org.dbunit.database.IDatabaseConnection;

import org.rhq.helpers.perftest.support.dbunit.DbUnitUtil;
import org.rhq.helpers.perftest.support.dbunit.ReplicatingDataSetConsumer;
import org.rhq.helpers.perftest.support.jpa.HibernateFacade;
import org.rhq.helpers.perftest.support.replication.ReplicationConfiguration;
import org.rhq.helpers.perftest.support.replication.ReplicationResult;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class Replicator {

    public static ReplicationResult run(ReplicationConfiguration configuration) throws Exception {
        HibernateFacade facade = new HibernateFacade();
        facade.initialize(Collections.emptyMap());
        
        IDatabaseConnection connection = DbUnitUtil.getConnection(configuration.getReplicationConfiguration().getSettings());
        
        return run(configuration, connection, facade);
    }
    
    public static ReplicationResult run(ReplicationConfiguration configuration, IDatabaseConnection connection, HibernateFacade hibernateFacade) throws Exception {
        ReplicatingDataSetConsumer consumer = new ReplicatingDataSetConsumer(connection, configuration, hibernateFacade);
                
        Exporter.run(configuration.getReplicationConfiguration(), consumer, connection);
        
        return consumer.getResult();
    }    
}
