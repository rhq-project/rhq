/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rhq.enterprise.server.storage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import org.codehaus.jackson.map.ObjectMapper;

import org.rhq.server.metrics.StorageSession;

/**
 * @author jsanda
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ReplicationSettingsManagerBean {

    @EJB
    private StorageClientManager storageClientManager;

    private PreparedStatement findOptions;

    private ObjectMapper mapper;

    @PostConstruct
    private void init() {
        StorageSession session = storageClientManager.getSession();
        findOptions = session.prepare("SELECT keyspace_name, strategy_options FROM system.schema_keyspaces " +
            "WHERE keyspace_name in ('rhq', 'system_auth')");
        mapper = new ObjectMapper();
    }

    public ReplicationSettings getReplicationSettings() {
        try {
            StorageSession session = storageClientManager.getSession();
            Map<String, Integer> replicationFactors = new HashMap<String,Integer>();
            ResultSet resultSet = session.execute(findOptions.bind());
            for (Row row : resultSet) {
                String keyspace = row.getString(0);
                if (keyspace.equals("rhq") || keyspace.equals("system_auth")) {
                    HashMap<String, String> options = mapper.readValue(row.getString(1), HashMap.class);
                    replicationFactors.put(keyspace, Integer.valueOf(options.get("replication_factor")));
                }
            }
            return new ReplicationSettings(replicationFactors);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
