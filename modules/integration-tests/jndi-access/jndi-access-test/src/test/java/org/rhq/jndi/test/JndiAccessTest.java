/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.jndi.test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import org.rhq.bindings.StandardScriptPermissions;
import org.rhq.enterprise.server.AllowRhqServerInternalsAccessPermission;
import org.rhq.enterprise.server.test.StrippedDownStartupBean;
import org.rhq.enterprise.server.test.StrippedDownStartupBeanPreparation;
import org.rhq.scripting.javascript.JsEngineProvider;
import org.rhq.scripting.python.PythonScriptEngineProvider;

/**
 * 
 * @author Lukas Krejci
 */
@Test
public class JndiAccessTest extends Arquillian {

    private static final Log JNP_SERVER_LOG = LogFactory.getLog("Test JNP Server");

    private static final String GROUP_FOR_NORMAL_TESTS = "JndiAccessTest";

    private Process testServerProcess;
    private Thread testServerStdErrReader;
    private Thread testServerStdOutReader;

    @Deployment
    public static EnterpriseArchive TestEar() {
        JavaArchive testEjb = ShrinkWrap.create(JavaArchive.class, "test-ejb.jar").addClass(TestEjb.class)
            .addClass(TestEjbBean.class).addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"));

        Collection<JavaArchive> deps = DependencyResolvers.use(MavenDependencyResolver.class)
            .loadMetadataFromPom("pom.xml")
            .artifacts("jboss:jnp-client", "org.rhq:rhq-scripting-api", "org.rhq:rhq-scripting-javascript",
                "org.rhq:rhq-scripting-python").scope("test").resolveAs(JavaArchive.class);

        //we need to pull in the naming hack classes from the server jar so that our EAR behaves the same
        testEjb.addPackages(true, "org.rhq.enterprise.server.naming").addClass(
            AllowRhqServerInternalsAccessPermission.class);
        
        //we also need to pull in the special startup beans from the server/itests-2 that will initialize the naming
        //subsystem
        testEjb.addClasses(StrippedDownStartupBean.class, StrippedDownStartupBeanPreparation.class);
        
        //to work around https://issues.jboss.org/browse/ARQ-659
        //we need to include this test class in the EAR manually

        //Instead of pulling the whole rhq-script-bindings (which has a lot of deps), we're just picking the
        //StandardScriptPermissions from there so that it can be used in the tests
        JavaArchive classes = ShrinkWrap.create(JavaArchive.class, "test-class.jar").addClasses(JndiAccessTest.class, 
            StandardScriptPermissions.class);

        return ShrinkWrap.create(EnterpriseArchive.class, "test-ear.ear").addAsModule(testEjb).addAsLibraries(deps)
            .addAsLibrary(classes);
    }

    @RunAsClient
    @Parameters({ "test.server.jar.path", "jnp.port", "jnp.rmiPort" })
    public void startTestJnpServer(String testServerJar, int jnpPort, int rmiPort) throws Exception {
        ProcessBuilder bld = new ProcessBuilder("java", "-Djnp.port=" + jnpPort, "-Djnp.rmiPort=" + rmiPort, "-jar",
            testServerJar);

        testServerProcess = bld.start();

        testServerStdErrReader = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader rdr = new BufferedReader(new InputStreamReader(testServerProcess.getErrorStream()));
                try {
                    String line;
                    while ((line = rdr.readLine()) != null) {
                        JNP_SERVER_LOG.warn(line);
                    }
                } catch (IOException e) {
                    JNP_SERVER_LOG.error("Reading test JNP server error output failed.", e);
                } finally {
                    try {
                        rdr.close();
                    } catch (IOException e) {
                        JNP_SERVER_LOG.error("Failed to close the test server error stream.", e);
                    }
                }
            }
        });
        testServerStdErrReader.start();

        testServerStdOutReader = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader rdr = new BufferedReader(new InputStreamReader(testServerProcess.getInputStream()));
                try {
                    String line;
                    while ((line = rdr.readLine()) != null) {
                        JNP_SERVER_LOG.debug(line);
                    }
                } catch (IOException e) {
                    JNP_SERVER_LOG.error("Reading test JNP server standard output failed.", e);
                } finally {
                    try {
                        rdr.close();
                    } catch (IOException e) {
                        JNP_SERVER_LOG.error("Failed to close the test server standard output stream.", e);
                    }
                }
            }
        });
        testServerStdOutReader.start();

        //give the JNP server some time to start up
        Thread.sleep(5000);
    }

    @RunAsClient
    @Test(dependsOnGroups = GROUP_FOR_NORMAL_TESTS, alwaysRun = true)
    public void stopTestJnpServer() throws Exception {
        testServerProcess.destroy();
        testServerStdErrReader.join();
        testServerStdOutReader.join();
    }

    @Test(dependsOnMethods = "startTestJnpServer", groups = GROUP_FOR_NORMAL_TESTS)
    @Parameters("jnp.port")
    public void testRemoteConnectionWorkingFromJava(int jnpPort) throws Exception {
        Properties env = new Properties();
        env.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
        env.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
        env.put("java.naming.provider.url", "jnp://localhost:" + jnpPort);
        InitialContext ctx = new InitialContext(env);
        Object kachny = ctx.lookup("kachny");

        assertNotNull(kachny);
    }

    @Test(dependsOnMethods = "startTestJnpServer", groups = GROUP_FOR_NORMAL_TESTS)
    @Parameters("jnp.port")
    public void testRemoteConnectionWorkingFromScripts_javascript(int jnpPort) throws Exception {
        getJavascriptScriptEngine().eval("" //
            + "var env = new java.util.Properties;" //
            + "env.put('java.naming.factory.initial', 'org.jnp.interfaces.NamingContextFactory');" //
            + "env.put('java.naming.factory.url.pkgs', 'org.jboss.naming:org.jnp.interfaces');" //
            + "env.put('java.naming.provider.url', 'jnp://localhost:" + jnpPort + "');" //
            + "var ctx = new javax.naming.InitialContext(env);" //
            + "var kachny = ctx.lookup('kachny');" //
            + "if (kachny == null) throw 'The object retrieved from the remote server should not be null';");
    }

    @Test(dependsOnMethods = "startTestJnpServer", groups = GROUP_FOR_NORMAL_TESTS)
    @Parameters("jnp.port")
    public void testRemoteConnectionWorkingFromScripts_python(int jnpPort) throws Exception {
        getPythonScriptEngine()
            .eval(
                "" //
                    + "import java.util as u\n" //
                    + "import javax.naming as n\n" //
                    + "env = u.Properties()\n" //
                    + "env.put('java.naming.factory.initial', 'org.jnp.interfaces.NamingContextFactory')\n" //
                    + "env.put('java.naming.factory.url.pkgs', 'org.jboss.naming:org.jnp.interfaces')\n" //
                    + "env.put('java.naming.provider.url', 'jnp://localhost:"
                    + jnpPort
                    + "')\n" //
                    + "ctx = n.InitialContext(env)\n" //
                    + "kachny = ctx.lookup('kachny')\n" //
                    + "if (kachny is None):\n raise Exception('The object retrieved from the remote server should not be null')\n");
    }

    @Test(dependsOnMethods = "startTestJnpServer", groups = GROUP_FOR_NORMAL_TESTS)
    public void testLocalJNDILookupWorksFromJava() throws Exception {
        InitialContext ctx = new InitialContext();
        Object bean = ctx.lookup("java:app/test-ejb/TestEjbBean");

        assertNotNull(bean);
    }

    @Test(dependsOnMethods = "startTestJnpServer", groups = GROUP_FOR_NORMAL_TESTS)
    public void testLocalJNDILookupFailsFromScripts_javascript() throws Exception {
        try {
            getJavascriptScriptEngine().eval(
                "var ctx = new javax.naming.InitialContext();\n"
                    + "var bean = ctx.lookup('java:app/test-ejb/TestEjbBean');\n");
            fail("Script was able to perform local JNDI lookup.");
        } catch (Exception e) {
            checkIsDesiredSecurityException(e);
        }
    }

    @Test(dependsOnMethods = "startTestJnpServer", groups = GROUP_FOR_NORMAL_TESTS)
    public void testLocalJNDILookupFailsFromScripts_python() throws Exception {
        try {
            getPythonScriptEngine().eval("import javax.naming as n\n" // 
                + "ctx = n.InitialContext()\n" // 
                + "bean = ctx.lookup('java:app/test-ejb/TestEjbBean')\n");
            fail("Script was able to perform local JNDI lookup.");
        } catch (Exception e) {
            checkIsDesiredSecurityException(e);
        }
    }

    private ScriptEngine getJavascriptScriptEngine() throws ScriptException {
        return new JsEngineProvider().getInitializer().instantiate(Collections.<String> emptySet(),
            new StandardScriptPermissions());
    }

    private ScriptEngine getPythonScriptEngine() throws ScriptException {
        return new PythonScriptEngineProvider().getInitializer().instantiate(Collections.<String> emptySet(),
            new StandardScriptPermissions());
    }

    private void checkIsDesiredSecurityException(Throwable t) {
        //TODO implement
        System.out.println(t);
    }
}
