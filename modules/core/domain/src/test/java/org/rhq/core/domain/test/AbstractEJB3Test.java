/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.test;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.Hashtable;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionManager;

import org.testng.AssertJUnit;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;

import org.jboss.ejb3.embedded.EJB3StandaloneBootstrap;
import org.jboss.ejb3.embedded.EJB3StandaloneDeployer;

public abstract class AbstractEJB3Test extends AssertJUnit {
    //    @BeforeSuite(groups = "integration.ejb3")
    @BeforeGroups(groups = "integration.ejb3")
    public static void startupEmbeddedJboss() {
        System.out.println("Starting ejb3...");
        String classesDir = System.getProperty("ejbjarDirectory", "target/classes");
        System.out.println("Loading EJB3 classes from directory: " + classesDir);
        try {
            EJB3StandaloneBootstrap.boot(null);
            EJB3StandaloneBootstrap.scanClasspath(classesDir);

            System.err.println("...... embedded-jboss-beans deployed....");

            // Add all EJBs found in the archive that has this file
            EJB3StandaloneDeployer deployer = new EJB3StandaloneDeployer();

            File deployment = new File(classesDir);
            if (!deployment.exists()) {
                System.err.println("Deployment directory does not exist: " + deployment.getAbsolutePath());
            }

            URL archive = deployment.toURI().toURL();
            deployer.getArchives().add(archive);

            System.err.println("...... deploying MM ejb3.....");
            System.err.println("...... ejb3 deployed....");

            // Deploy everything we got
            deployer.setKernel(EJB3StandaloneBootstrap.getKernel());
            deployer.create();
            System.err.println("...... deployer created....");

            deployer.start();
            System.err.println("...... deployer started....");
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    //    @AfterSuite
    @AfterGroups(groups = "integration.ejb3")
    public static void shutdownEmbeddedJboss() {
        EJB3StandaloneBootstrap.shutdown();
    }

    private TransactionManager tm;

    public TransactionManager getTransactionManager() {
        InitialContext initialContext = null;
        try {
            initialContext = getInitialContext();
            tm = (TransactionManager) initialContext.lookup("java:/TransactionManager");
            return tm;
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load transaction manager", e);
        } finally {
            try {
                initialContext.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to close the initial context - why did this happen?", e);
            }
        }
    }

    public EntityManager getEntityManager() {
        InitialContext initialContext = null;
        try {
            initialContext = getInitialContext();
            return ((EntityManagerFactory) initialContext.lookup("java:/RHQEntityManagerFactory"))
                .createEntityManager();
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load entity manager", e);
        } finally {
            try {
                initialContext.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to close the initial context - why did this happen?", e);
            }
        }
    }

    public boolean isPostgres(EntityManager em) throws Exception {
        return ((org.hibernate.ejb.EntityManagerImpl) em).getSession().connection().getMetaData()
            .getDatabaseProductName().toLowerCase().indexOf("postgres") > -1;
    }

    public InitialContext getInitialContext() {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
        env.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
        try {
            return new InitialContext(env);
        } catch (NamingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load initial context", e);
        }
    }

    private final long DEFAULT_OFFSET = 50;
    private long referenceTime = new Date().getTime();

    public Date getAnotherDate() {
        return getAnotherDate(DEFAULT_OFFSET);
    }

    public Date getAnotherDate(long offset) {
        referenceTime += offset;
        return new Date(referenceTime);
    }
}