/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.core.domain.test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.transaction.TransactionManager;

import org.testng.AssertJUnit;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;

import org.rhq.core.domain.shared.TransactionCallback;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.test.AssertUtils;
import org.rhq.test.MatchResult;
import org.rhq.test.PropertyMatchException;
import org.rhq.test.PropertyMatcher;

public abstract class AbstractEJB3Test extends Arquillian {

    protected static final String JNDI_RHQDS = "java:jboss/datasources/RHQDS";

    @PersistenceContext
    protected EntityManager em;

    @ArquillianResource
    protected InitialContext initialContext;

    // We originally deployed the domain jar as a JavaArchive (JAR file). But this ran into problems because
    // of the dependent 3rd party jars.  There isn't an obvious way to deploy the dependent jars of a jar.
    // Also, AS7 makes it hard to put them in a globally accessibly /lib directory due to
    // its classloader isolation and module approach. So, we now deploy an EnterpriseArchive (ear)
    // where the domain jar is deployed as a module and the 3rd party libraries are put in the ear's /lib.

    @Deployment
    protected static EnterpriseArchive getBaseDeployment() {

        // Create the domain jar which will subsequently be packaged in the ear deployment   
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "rhq-core-domain-ejb3.jar") //
            .addAsManifestResource("ejb-jar.xml") // the empty ejb jar descriptor
            .addAsManifestResource("test-persistence.xml", "persistence.xml") //  the test persistence context            
            .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml") // add CDI injection (needed by arquillian injection)
            );

        // non-domain RHQ classes in use by domain test classes
        ejbJar.addClass(ThrowableUtil.class) //
            .addClass(MessageDigestGenerator.class) //
            .addClass(StreamUtil.class) //
            .addClass(AssertUtils.class) //
            .addClasses(PropertyMatcher.class, MatchResult.class, PropertyMatchException.class);

        // domain classes
        ejbJar = addClasses(ejbJar, new File("target/classes/org"), null);

        // SetupBean class
        ejbJar.addClass(SetupBean.class);

        JavaArchive testClassesJar = ShrinkWrap.create(JavaArchive.class, "test-classes.jar");
        testClassesJar = addClasses(testClassesJar, new File("target/test-classes/org"), null);

        // create test ear
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test-domain.ear") //
            .addAsModule(ejbJar) // the domain jar under test
            .addAsLibrary(testClassesJar) // the actual test classes
            .setApplicationXML("application.xml"); // the application xml declaring the ejb jar

        // Adding the 3rd party jars is not easy.  There were basically two approaches I could think of:
        //
        // 1) Use the MavenDependencyResolver to figure out and add all of the test deps and put them in lib
        // 
        // This immediately ran into trouble as there were way more jars sucked in than were actually necessary.
        // Furthermore, it included arquillian, shrinkwrap, jboss, etc.. lots of jars that actually caused
        // issues like locking and other horrible things due, I suppose, to just stepping all over the test env
        // set up by Arquillian.  So, the next step was to try and start excluding the unwanted jars.  This was
        // tedious and difficult and I didn't really get it close to working before giving up and going with
        // option 2.
        //
        // 2) Use the MavenDependencyResolver to locate and pull just the artifacts we need.
        //
        // This is annoying because it's basically duplicating the same sort of effort that we already
        // use maven to do for us. It involves running, failing on NoClassDefFound, fixing it, repeat. It
        // does pull in necessary transitive deps but sometimes it pulls in unwanted transitive deps. So,
        // we still end up having to do some exclusion filtering.  Since Shrinkwrap has weak and buggy         
        // filtering, we have some homegrown filtering methods below. 
        // TODO: Is there any way to not have to specify the versions for the transitive deps? This is brittle as is.
        //       Can pass the version in as a system prop if necessary...

        //load 3rd party deps explicitly
        Collection<String> thirdPartyDeps = new ArrayList<String>();
        thirdPartyDeps.add("commons-beanutils:commons-beanutils:1.8.2");
        thirdPartyDeps.add("commons-codec:commons-codec");
        thirdPartyDeps.add("commons-io:commons-io");
        thirdPartyDeps.add("org.unitils:unitils-testng:3.1");
        thirdPartyDeps.add("org.rhq:rhq-core-dbutils:" + System.getProperty("project.version")); // needed by SetupBean

        MavenResolverSystem resolver = Maven.resolver();

        Collection<JavaArchive> dependencies = new HashSet<JavaArchive>();
        dependencies.addAll(Arrays.asList(resolver.loadPomFromFile("pom.xml").resolve(thirdPartyDeps)
            .withTransitivity().as(JavaArchive.class)));

        String[] excludeFilters = { "testng.*jdk" };

        dependencies = exclude(dependencies, excludeFilters);
        ear = ear.addAsLibraries(dependencies);

        System.out.println("** The Deployment EAR: " + ear.toString(true) + "\n");

        return ear;
    }

    /**
     * @param dependencies The current set of deps - THIS IS FILTERED, not copied
     * @param filters regex filters
     * @return the filtered set of deps
     */
    public static Collection<JavaArchive> exclude(Collection<JavaArchive> dependencies, String... filters) {

        for (String filter : filters) {
            Pattern p = Pattern.compile(filter);

            for (Iterator<JavaArchive> i = dependencies.iterator(); i.hasNext();) {
                JavaArchive dependency = i.next();
                if (p.matcher(dependency.getName()).find()) {
                    i.remove();
                }
            }
        }

        return dependencies;
    }

    /**
     * @param dependencies The current set of deps - this is not changed
     * @param filters regex filters
     * @return set set of JavaArchives in dependencies that matched an include filter
     */
    public static Collection<JavaArchive> include(Collection<JavaArchive> dependencies, String... filters) {

        Collection<JavaArchive> result = new HashSet<JavaArchive>();

        for (String filter : filters) {
            Pattern p = Pattern.compile(filter);

            for (JavaArchive dependency : dependencies) {
                if (p.matcher(dependency.getName()).find()) {
                    result.add(dependency);
                }
            }
        }

        return result;
    }

    public static JavaArchive addClasses(JavaArchive archive, File dir, String packageName) {

        packageName = (null == packageName) ? "" + dir.getName() : packageName + "." + dir.getName();

        for (File file : dir.listFiles()) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                archive = addClasses(archive, file, packageName);
            } else if (fileName.endsWith(".class")) {
                int dot = fileName.indexOf('.');
                try {
                    archive.addClass(packageName + "." + fileName.substring(0, dot));
                } catch (Exception e) {
                    System.out.println("WARN: Could not add class:" + e);
                }
            }
        }

        return archive;
    }

    /**
     * <p>DO NOT OVERRIDE.</p>
     * <p>DO NOT DEFINE AN @BeforeMethod</p>
     * 
     * Instead, override {@link #beforeMethod()}.  If you must override, for example, if you
     * need to use special attributes on your annotation, then ensure you protect the code with
     * and {@link #inContainer()} call.
     */
    @BeforeMethod
    protected void __beforeMethod(Method method) throws Throwable {
        // Note that Arquillian calls the testng BeforeMethod twice (as of 1.0.2.Final, once 
        // out of container and once in container. In general the expectation is to execute it 
        // one time, and doing it in container allows for the expected injections and context.
        if (inContainer()) {
            try {
                beforeMethod();

            } catch (Throwable t) {
                // Arquillian is eating these, make sure they show up in some way
                System.out.println("BEFORE METHOD FAILURE, TEST DID NOT RUN!!! [" + method.getName() + "]");
                t.printStackTrace();
                throw t;
            }
        }
    }

    /**
     * <p>DO NOT OVERRIDE.</p>
     * <p>DO NOT DEFINE AN @AfterMethod</p>
     * 
     * Instead, override {@link #afterMethod()}.
     */
    @AfterMethod(alwaysRun = true)
    protected void __afterMethod(ITestResult result, Method method) throws Throwable {
        try {
            if (inContainer()) {
                afterMethod();
            }
        } catch (Throwable t) {
            System.out
                .println("AFTER METHOD FAILURE, TEST CLEAN UP FAILED!!! MAY NEED TO CLEAN DB BEFORE RUNNING MORE TESTS! ["
                    + method.getName() + "]");
            t.printStackTrace();
            throw t;
        }
    }

    protected boolean inContainer() {
        // If the injection is done we're running in the container. 
        return (null != initialContext);
    }

    /**
     * Override Point!  Do not implement a @BeforeMethod, instead override this method. 
     */
    protected void beforeMethod() throws Exception {
        // do nothing if we're not overridden
    }

    /**
     * Override Point!  Do not implement an @AfterMethod, instead override this method. 
     */
    protected void afterMethod() throws Exception {
        // do nothing if we're not overridden
    }

    protected void startTransaction() throws Exception {
        getTransactionManager().begin();
    }

    protected void commitTransaction() throws Exception {
        getTransactionManager().commit();
    }

    protected void rollbackTransaction() throws Exception {
        getTransactionManager().rollback();
    }

    protected InitialContext getInitialContext() {
        // may be null if not yet injected
        if (null != initialContext) {
            return initialContext;
        }

        InitialContext result;

        try {
            Properties jndiProperties = new Properties();
            jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.as.naming.InitialContextFactory");
            result = new InitialContext(jndiProperties);

        } catch (Exception e) {
            throw new RuntimeException("Failed to get InitialContext", e);
        }

        return result;
    }

    public TransactionManager getTransactionManager() {
        TransactionManager result;

        try {
            result = (TransactionManager) getInitialContext().lookup("java:jboss/TransactionManager");
            if (null == result) {
                result = (TransactionManager) getInitialContext().lookup("TransactionManager");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to get TransactionManager", e);
        }

        return result;
    }

    protected EntityManager getEntityManager() {
        // may be null if not yet injected (as of 1.0.1.Final, only injected inside @Test)
        if (null != em) {
            return em;
        }

        EntityManager result;

        try {
            EntityManagerFactory emf = (EntityManagerFactory) getInitialContext().lookup(
                "java:jboss/RHQEntityManagerFactory");
            result = emf.createEntityManager();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get EntityManagerFactory", e);
        }

        return result;
    }

    /**
     * Equivalent to <pre>executeInTransaction(true, callback)</pre>.
     * @param callback
     */
    protected void executeInTransaction(TransactionCallback callback) {
        executeInTransaction(true, callback);
    }

    protected void executeInTransaction(boolean rollback, TransactionCallback callback) {
        try {
            startTransaction();
            callback.execute();

        } catch (Throwable t) {
            rollback = true;
            RuntimeException re = new RuntimeException(ThrowableUtil.getAllMessages(t), t);
            throw re;

        } finally {
            try {
                if (!rollback) {
                    commitTransaction();
                } else {
                    rollbackTransaction();
                }
            } catch (Exception e) {
                //noinspection ThrowFromFinallyBlock
                throw new RuntimeException("Failed to " + (rollback ? "rollback" : "commit") + " transaction", e);
            }
        }
    }

    /* The old AbstractEJB3Test impl extended AssertJUnit. Continue to support the used methods
     * with various call-thru methods. 
     */

    protected void assertNotNull(String msg, Object o) {
        AssertJUnit.assertNotNull(msg, o);
    }

    protected void assertNull(String msg, Object o) {
        AssertJUnit.assertNull(msg, o);
    }

    protected void assertFalse(String msg, boolean b) {
        AssertJUnit.assertFalse(msg, b);
    }

    protected void assertTrue(String msg, boolean b) {
        AssertJUnit.assertTrue(msg, b);
    }

    protected void assertEquals(String msg, int expected, int actual) {
        AssertJUnit.assertEquals(msg, expected, actual);
    }

    protected void assertEquals(String msg, String expected, String actual) {
        AssertJUnit.assertEquals(msg, expected, actual);
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

    private static final long DEFAULT_OFFSET = 50;
    private long referenceTime = new Date().getTime();

    public Date getAnotherDate() {
        return getAnotherDate(DEFAULT_OFFSET);
    }

    public Date getAnotherDate(long offset) {
        referenceTime += offset;
        return new Date(referenceTime);
    }

}
