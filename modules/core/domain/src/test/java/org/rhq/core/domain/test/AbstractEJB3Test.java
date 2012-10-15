package org.rhq.core.domain.test;

import java.io.File;
import java.util.Date;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.transaction.TransactionManager;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.rhq.core.domain.drift.DriftDataAccessTest;
import org.rhq.core.domain.util.TransactionCallback;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;

public abstract class AbstractEJB3Test extends Arquillian {

    protected static final String JNDI_RHQDS = "java:jboss/datasources/RHQDS";

    @PersistenceContext
    protected EntityManager em;

    @ArquillianResource
    protected InitialContext initialContext;

    @Deployment
    protected static JavaArchive getBaseDeployment() {

        String dialect = System.getProperty("hibernate.dialect");
        if (dialect == null) {
            System.out.println("!!! hibernate.dialect is not set! Assuming you want to test on postgres");
            dialect = "postgres";
        }

        String dataSourceXml;
        if (dialect.toLowerCase().contains("postgres")) {
            dataSourceXml = "jbossas-postgres-ds.xml";
        } else {
            dataSourceXml = "jbossas-oracle-ds.xml";
        }

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "test-domain.jar") //
            .addAsManifestResource(dataSourceXml)
            // add the test persistence context
            .addAsManifestResource("test-persistence.xml", "persistence.xml")
            // add CDI injection (needed by arquillian injection)
            .addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
            // add necessary classes
            // TODO: Why are these needed here and not taken care of in the domain call addition below  
            .addClass(TransactionCallback.class) //
            .addClass(DriftDataAccessTest.class) //
            .addClass(AbstractEJB3Test.class);

        // non-domain classes in use by domain test classes
        jar.addClass(ThrowableUtil.class) //
            .addClass(MessageDigestGenerator.class) //
            .addClass(StreamUtil.class);

        // domain classes
        jar = addClasses(jar, new File("target/classes/org"), null);

        return jar;
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
                    Class<?> clazz = Class.forName(packageName + "." + fileName.substring(0, dot));
                    archive.addClasses(clazz);
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
    protected void __beforeMethod() throws Exception {
        // Note that Arquillian calls the testng BeforeMethod twice (as of 1.0.2.Final, once 
        // out of container and once in container. In general the expectation is to execute it 
        // one time, and doing it in container allows for the expected injections and context.
        if (inContainer()) {
            beforeMethod();
        }
    }

    /**
     * <p>DO NOT OVERRIDE.</p>
     * <p>DO NOT DEFINE AN @AfterMethod</p>
     * 
     * Instead, override {@link #afterMethod()}.
     */
    @BeforeMethod
    protected void __afterMethod() throws Exception {
        // currently no special handling necessary
        afterMethod();
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
        // may be null if not yet injected (as of 1.0.1.Final, only injected inside @Test)
        if (null != initialContext) {
            return initialContext;
        }

        InitialContext result = null;

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
        TransactionManager result = null;

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

        EntityManager result = null;

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
            RuntimeException re = new RuntimeException(ThrowableUtil.getAllMessages(t), t);
            re.printStackTrace();
            throw re;

        } finally {
            try {
                if (!rollback) {
                    commitTransaction();
                } else {
                    rollbackTransaction();
                }
            } catch (Exception e) {
                System.out.println("Failed to " + (rollback ? "rollback" : "commit") + " transaction: " + e);
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
        AssertJUnit.assertEquals(expected, actual);
    }

    protected void assertEquals(String msg, String expected, String actual) {
        AssertJUnit.assertEquals(expected, actual);
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
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
