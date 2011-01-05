/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.jpa;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DialectFactory;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.packaging.PersistenceMetadata;
import org.hibernate.ejb.packaging.PersistenceXmlLoader;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.mapping.PersistentClass;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class HibernateFacade {

    private Map<Ejb3Configuration, SessionFactoryImplementor> sessionFactoryByPu;

    public void initialize(Map<Object, Object> overrides) throws Exception {
        sessionFactoryByPu = new HashMap<Ejb3Configuration, SessionFactoryImplementor>();
        Enumeration<URL> persistenceXmls = getClass().getClassLoader().getResources("META-INF/persistence.xml");

        Ejb3Configuration configuration = new Ejb3Configuration();

        AnnotationConfiguration annotationConfiguration = configuration.getHibernateConfiguration();

        while (persistenceXmls.hasMoreElements()) {
            URL persistenceXml = persistenceXmls.nextElement();
            List<PersistenceMetadata> metadataFiles = PersistenceXmlLoader.deploy(persistenceXml, overrides,
                annotationConfiguration.getEntityResolver());

            for (PersistenceMetadata metadata : metadataFiles) {
                String pu = metadata.getName();

                Ejb3Configuration puConfig = new Ejb3Configuration();
                puConfig = puConfig.configure(pu, Collections.emptyMap());

                puConfig.getProperties().clear();
                
                puConfig.getProperties().putAll(overrides);
                
                if (puConfig != null) {
                    sessionFactoryByPu.put(puConfig, null);
                }
            }
        }
    }

    public SessionFactoryImplementor getSessionFactory(Class<?> entity, Connection connection) throws SQLException {
        Ejb3Configuration config = getPUConfiguration(entity);
        if (config == null) {
            return null;
        }
        
        SessionFactoryImplementor factory = sessionFactoryByPu.get(config);
        if (factory == null) {
            //get the dialect name manually from the connection
            if (config.getProperties().get(Environment.DIALECT) == null) {
                String databaseName = connection.getMetaData().getDatabaseProductName();
                int majorVersion = connection.getMetaData().getDatabaseMajorVersion();
                
                Dialect dialect = DialectFactory.determineDialect(databaseName, majorVersion);
                
                config.getProperties().put(Environment.DIALECT, dialect.getClass().getName());
            }
            
            factory = (SessionFactoryImplementor) config.getHibernateConfiguration().buildSessionFactory();
            sessionFactoryByPu.put(config, factory);
        }
        
        return factory;
    }
    
    public Session getSession(Class<?> entity, Connection connection) throws SQLException {        
        return getSessionFactory(entity, connection).openSession(connection);
    }
    
    public PersistentClass getPersistentClass(Class<?> entity) {
        Ejb3Configuration config = getPUConfiguration(entity);

        if (config == null) {
            return null;
        }

        return config.getClassMapping(entity.getName());
    }

    private Ejb3Configuration getPUConfiguration(Class<?> entity) {
        for (Ejb3Configuration config : sessionFactoryByPu.keySet()) {
            if (config.getClassMapping(entity.getName()) != null) {
                return config;
            }
        }

        return null;
    }
}
