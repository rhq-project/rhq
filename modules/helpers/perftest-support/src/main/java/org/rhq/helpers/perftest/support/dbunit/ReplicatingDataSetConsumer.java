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

package org.rhq.helpers.perftest.support.dbunit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.ITableMetaData;
import org.dbunit.dataset.stream.IDataSetConsumer;
import org.hibernate.Session;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.persister.entity.EntityPersister;

import org.rhq.helpers.perftest.support.Util;
import org.rhq.helpers.perftest.support.config.Entity;
import org.rhq.helpers.perftest.support.config.ExportConfiguration;
import org.rhq.helpers.perftest.support.jpa.ConfigurableDependencyInclusionResolver;
import org.rhq.helpers.perftest.support.jpa.Edge;
import org.rhq.helpers.perftest.support.jpa.EntityDependencyGraph;
import org.rhq.helpers.perftest.support.jpa.HibernateFacade;
import org.rhq.helpers.perftest.support.jpa.JPAUtil;
import org.rhq.helpers.perftest.support.jpa.Node;
import org.rhq.helpers.perftest.support.replication.ReplicaModifier;
import org.rhq.helpers.perftest.support.replication.ReplicationConfiguration;
import org.rhq.helpers.perftest.support.replication.ReplicationResult;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ReplicatingDataSetConsumer implements IDataSetConsumer {

    private ReplicaModifier modifier;
    private IDatabaseConnection connection;
    private ReplicationResult result;
    private Map<String, Class<?>> tableToEntityMap;
    private EntityDependencyGraph edg;
    private ConfigurableDependencyInclusionResolver inclusionResolver;
    private ITableMetaData currentTableMetaData;
    private HibernateFacade facade;
    private Map<Class<?>, Map<Object, Object>> replicaIdsPerEntity;
    private Session currentSession;
    private IdentifierGenerator currentGenerator;
    
    /**
     * @param idProvider
     * @param modifier
     * @param connection
     */
    public ReplicatingDataSetConsumer(IDatabaseConnection connection, ReplicationConfiguration configuration,
        HibernateFacade hibernateFacade) {
        this.modifier = configuration.getModifier();
        this.connection = connection;
        this.result = new ReplicationResult();

        ExportConfiguration graphConfig = configuration.getReplicationConfiguration();
        inclusionResolver = new ConfigurableDependencyInclusionResolver(graphConfig);

        Map<Entity, String> entityQueries = Util.getEntityQueries(graphConfig);

        edg = new EntityDependencyGraph();

        for (Map.Entry<Entity, String> entry : entityQueries.entrySet()) {
            Entity entity = entry.getKey();

            edg.addEntity(graphConfig.getClassForEntity(entity));
        }

        tableToEntityMap = new HashMap<String, Class<?>>();

        for (Node n : edg.getAllNodes()) {
            tableToEntityMap.put(n.getTranslation().getTableName(), n.getEntity());
        }

        replicaIdsPerEntity = new HashMap<Class<?>, Map<Object, Object>>();

        facade = hibernateFacade;
    }

    public ReplicationResult getResult() {
        return result;
    }

    public void startDataSet() throws DataSetException {
    }

    public void endDataSet() throws DataSetException {
    }

    public void startTable(ITableMetaData metaData) throws DataSetException {
        currentTableMetaData = metaData;
        Class<?> entityClass = getCurrentEntityClass();
        try {
            SessionFactoryImplementor factory = facade.getSessionFactory(entityClass, connection.getConnection());
            EntityPersister persister = factory.getEntityPersister(entityClass.getName());
            currentGenerator = persister.getIdentifierGenerator();
            currentSession = facade.getSession(entityClass, connection.getConnection());
        } catch (SQLException e) {
            throw new DataSetException(e);
        }
    }

    public void endTable() throws DataSetException {
        currentSession.close();
        currentSession = null;
        currentGenerator = null;
    }

    public void row(Object[] values) throws DataSetException {
        storeResult(values);
    }

    private Object instantiateEntity(Object[] rowValues) throws Exception {
        Class<?> entityClass = getCurrentEntityClass();

        if (entityClass == null) {
            return null;
        }

        //we depend on an JPA entities having a no-arg constructor
        Constructor<?> constructor = entityClass.getConstructor();
        constructor.setAccessible(true);
        Object entity = constructor.newInstance();

        @SuppressWarnings("unchecked")
        Set<Field> jpaFields = JPAUtil.getJPAFields(entityClass, Column.class);

        for (Field f : jpaFields) {
            String name = f.getAnnotation(Column.class).name();

            int index = currentTableMetaData.getColumnIndex(name);

            f.setAccessible(true);
            f.set(entity, rowValues[index]);
        }

        return entity;
    }

    private void storeResult(Object[] originalValues) throws DataSetException {
        try {
            Object original = instantiateEntity(originalValues);

            Class<?> entityClass = getCurrentEntityClass();
            Node node = edg.getNode(entityClass);

            for (Edge e : node.getIncomingEdges()) {
                if (e.getToField() != null) {
                    //TODO use the edge's translation to get column names to update
                    //and look up the existing replica ids to replace the value
                    //in the original values with.
                }
            }

            String[] pks = node.getTranslation().getPkColumns();
            if (pks == null || pks.length == 0) {
                throw new IllegalStateException("A table without a primary key?");
            }
            if (pks.length > 1) {
                //TODO this should be implemented
                throw new IllegalStateException("Composite primary keys not supported.");
            }

            Object newId = currentGenerator.generate((SessionImplementor) currentSession, original);

            originalValues[currentTableMetaData.getColumnIndex(pks[0])] = newId;

            StringBuilder sql = new StringBuilder("INSERT INTO " + currentTableMetaData.getTableName() + " (");

            for (org.dbunit.dataset.Column c : currentTableMetaData.getColumns()) {
                sql.append(c.getColumnName()).append(", ");
            }

            sql.replace(sql.length() - 2, sql.length(), ") VALUES (");

            for (org.dbunit.dataset.Column c : currentTableMetaData.getColumns()) {
                sql.append("?, ");
            }

            sql.replace(sql.length() - 2, sql.length(), ")");

            PreparedStatement st = connection.getConnection().prepareStatement(sql.toString());

            int i = 0;
            for (org.dbunit.dataset.Column c : currentTableMetaData.getColumns()) {
                c.getDataType().setSqlValue(originalValues[i], i++, st);
            }

            st.execute();
        } catch (Exception e) {
            throw new DataSetException("Failed to replicate data.", e);
        }
    }

    private Class<?> getCurrentEntityClass() {
        return tableToEntityMap.get(currentTableMetaData.getTableName());
    }

    private Map<Object, Object> getReplicaIds(Class<?> entity) {
        Map<Object, Object> ret = replicaIdsPerEntity.get(entity);

        if (ret == null) {
            ret = new HashMap<Object, Object>();
            replicaIdsPerEntity.put(entity, ret);
        }

        return ret;
    }
}
