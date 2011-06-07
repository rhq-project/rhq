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

package org.rhq.helpers.inventoryserializer;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.log4j.Logger;

import org.rhq.enterprise.server.util.HibernateDetachUtility;
import org.rhq.helpers.inventoryserializer.util.ChildFirstClassLoader;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class Main {

    private static final Logger log = Logger.getLogger(Main.class);

    private Main() {

    }

    private static void usage() {
        System.out.println("Usage: inventory-serializer.[sh|bat] -c [-diupjs] <JPQL Query> [more JPQL quries]");
        System.out.println("Required arguments:");
        System.out.println("-c --connection-url : The JDBC connection URL to the database.");
        System.out.println();
        System.out.println("Optional arguments:");
        System.out.println("-d --driver-class : The class of the JDBC driver to use. The driver must be on the classpath. Defaults to org.postgresql.Driver.");
        System.out.println("-i --dialect : The Hibernate dialect to use. Defaults to org.hibernate.dialect.PosgreSQLDialect.");
        System.out.println("-u --username : The username to use when connecting to the database.");
        System.out.println("-p --password : The database password.");
        System.out.println("-j --jars : The comma-separated list of jars containing the entities. If none specified, the current classpath is used.");
        System.out.println("-s --persistence-unit : The persistence unit to execute the queries with. Defaults to 'rhqpu'.");
    }

    /**
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {
        LongOpt[] longOptions = new LongOpt[7];

        longOptions[0] = new LongOpt("driver-class", LongOpt.OPTIONAL_ARGUMENT, null, 'd');
        longOptions[1] = new LongOpt("dialect", LongOpt.OPTIONAL_ARGUMENT, null, 'i');
        longOptions[2] = new LongOpt("connection-url", LongOpt.REQUIRED_ARGUMENT, null, 'c');
        longOptions[3] = new LongOpt("username", LongOpt.OPTIONAL_ARGUMENT, null, 'u');
        longOptions[4] = new LongOpt("password", LongOpt.OPTIONAL_ARGUMENT, null, 'p');
        longOptions[5] = new LongOpt("jars", LongOpt.OPTIONAL_ARGUMENT, null, 'j');
        longOptions[6] = new LongOpt("persistence-unit", LongOpt.OPTIONAL_ARGUMENT, null, 's');

        Getopt options = new Getopt("iventory-serializer serializer", args, "d:i:c:u:p:j:s:", longOptions);

        String driverClass = "org.postgresql.Driver";
        String dialect = "org.hibernate.dialect.PostgreSQLDialect";
        String connectionUrl = null;
        String username = null; 
        String password = null;
        String persistenceUnit = "rhqpu";
        List<String> jars = new ArrayList<String>();
        List<String> queries = new ArrayList<String>();

        int option;
        while ((option = options.getopt()) != -1) {
            switch (option) {
            case 'd':
                driverClass = options.getOptarg();
                break;
            case 'i':
                dialect = options.getOptarg();
                break;
            case 'c':
                connectionUrl = options.getOptarg();
                break;
            case 'u':
                username = options.getOptarg();
                break;
            case 'p':
                password = options.getOptarg();
                break;
            case 'j':
                jars.addAll(extractCommaSeparated(options.getOptarg()));
                break;
            case 's':
                persistenceUnit = options.getOptarg();
            }
        }

        for (int i = options.getOptind(); i < args.length; i++) {
            queries.add(args[i]);
        }

        if (driverClass == null || dialect == null || connectionUrl == null || queries.isEmpty()) {
            usage();
            System.exit(1);
        }

        ClassLoader classLoaderToUse = Main.class.getClassLoader();

        if (jars.size() > 0) {
            URL[] jarUrls = getUrls(jars);
            classLoaderToUse = new ChildFirstClassLoader(jarUrls, Main.class.getClassLoader());
        }

        //these will collect the results and classes of the results
        List<Object> allResults = new ArrayList<Object>();
        Set<Class<?>> classes = new HashSet<Class<?>>();

        EntityManager em = null;
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoaderToUse);
            em = getEntityManager(driverClass, dialect, connectionUrl, username, password, persistenceUnit);
            
            for (String query : queries) {
                log.info("Executing query: " + query);

                Query q = em.createQuery(query);

                @SuppressWarnings("unchecked")
                List<Object> results = q.getResultList();

                allResults.addAll(results);
            }

            em.clear();

            for (Object result : allResults) {
                classes.add(result.getClass());
                HibernateDetachUtility.nullOutUninitializedFields(result, HibernateDetachUtility.SerializationType.JAXB);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }

        PrintStream out = System.out;
        out = new PrintStream(out, true, "UTF-8");

        out.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        out.append("<inventory-dump>\n");

        out.append("<classes-used>\n");

        for (Class<?> cls : classes) {
            out.append("<class>").append(cls.getName()).append("</class>\n");
        }

        out.append("</classes-used>\n");

        out.append("<objects>\n");

        JAXBContext context = JAXBContext.newInstance(classes.toArray(new Class<?>[classes.size()]));

        Marshaller marshaller = context.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");

        for (Object r : allResults) {
            marshaller.marshal(r, out);
        }

        out.append("</objects>\n");

        out.append("</inventory-dump>\n");
    }

    /**
     * @param jars
     * @return
     */
    private static URL[] getUrls(List<String> jars) {
        URL[] ret = new URL[jars.size()];

        int idx = 0;
        for (String jar : jars) {
            File f = new File(jar);
            if (!f.exists()) {
                throw new IllegalArgumentException("Could not find the jar '" + jar + "'.");
            }

            try {
                ret[idx++] = f.toURI().toURL();
            } catch (MalformedURLException e) {
                //doesn't happen
            }
        }

        return ret;
    }

    private static EntityManager getEntityManager(String driverClass, String dialect, String connectionUrl,
        String username, String password, String persistenceUnit) {

        HashMap<String, String> overrides = new HashMap<String, String>();

        overrides.put("javax.persistence.jtaDataSource", null);
        overrides.put("javax.persistence.nonJtaDataSource", null);
        overrides.put("hibernate.dialect", dialect);
        overrides.put("hibernate.connection.driver_class", driverClass);
        overrides.put("hibernate.connection.url", connectionUrl);
        overrides.put("hibernate.connection.user", username);
        overrides.put("hibernate.connection.password", password);

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(persistenceUnit, overrides);

        return emf.createEntityManager(overrides);
    }

    private static List<String> extractCommaSeparated(String arg) {
        String[] vals = arg.split("\\s*,\\s*");

        return Arrays.asList(vals);
    }
}
