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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import org.rhq.bindings.ScriptEngineFactory;
import org.rhq.bindings.StandardBindings;
import org.rhq.bindings.StandardScriptPermissions;
import org.rhq.bindings.util.PackageFinder;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.LocalClient;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.jndi.AllowRhqServerInternalsAccessPermission;
import org.rhq.test.PortScout;

/**
 *
 * !!!!!!! THIS TEST CLASS HAS BEEN MOVED TO ENTERPRISE/SERVER/ITESTS-2 !!!!!!
 * 
 * TODO: since this class has been updated and now differs from the version in itests-2 it is left here for
 *       reference. Also, the jndi access strategy needs to be updated for AS7.  When all is settled, this
 *       whole src branch should be deleted. 
 *
 * @author Lukas Krejci
 */
@Test
public class JndiAccessTest extends AbstractEJB3Test {
    private static final Log JNP_SERVER_LOG = LogFactory.getLog("Test JNP Server");
    private static final Log LOG = LogFactory.getLog(JndiAccessTest.class);
    
    private Process testServerProcess;
    private Thread testServerStdErrReader;
    private Thread testServerStdOutReader;
    
    int jnpPort;

    @BeforeClass
    @Parameters({ "test.server.jar.path" })
    public void startTestJnpServer(String testServerJar) throws Exception {
        int rmiPort = 0;
        PortScout scout = new PortScout();

        try {
            jnpPort = scout.getNextFreePort();
            rmiPort = scout.getNextFreePort();
        } finally {
            scout.close();
        }

        LOG.info("Starting the remote JNP server on jnpPort=" + jnpPort + ", rmiPort=" + rmiPort);
        ProcessBuilder bld = new ProcessBuilder("java", "-Djnp.port=" + jnpPort, "-Djnp.rmiPort=" + rmiPort, "-jar", testServerJar);
        
        testServerProcess = bld.start();
        
        testServerStdErrReader = new Thread(new Runnable() {
            @Override
            public void run() {
                BufferedReader rdr = new BufferedReader(new InputStreamReader(testServerProcess.getErrorStream()));
                try {
                    String line;
                    while((line = rdr.readLine()) != null) {
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
                    while((line = rdr.readLine()) != null) {
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
    
    @AfterClass
    public void stopTestJnpServer() throws Exception {
        testServerProcess.destroy();
        testServerStdErrReader.join();
        testServerStdOutReader.join();
    }
    
    public void testRemoteConnectionWorkingFromJava() throws Exception {
        Properties env = new Properties();
        env.put("java.naming.factory.initial", "org.jboss.naming.NamingContextFactory");
        env.put("java.naming.provider.url", "jnp://localhost:" + jnpPort);
        InitialContext ctx = new InitialContext(env);
        Object kachny = ctx.lookup("kachny");
        
        assert kachny != null;
    }
    
    public void testLocalJNDILookupFailsFromScripts() throws Exception {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();        
        
        ScriptEngine engine = getEngine(overlord);

        try {
            engine.eval(""
                + "var ctx = new javax.naming.InitialContext();\n"
                + "var entityManagerFactory = ctx.lookup('java:/RHQEntityManagerFactory');\n"
                + "var entityManager = entityManagerFactory.createEntityManager();\n"
                + "entityManager.find(java.lang.Class.forName('org.rhq.core.domain.resource.Resource'), java.lang.Integer.valueOf('10001'));");
            
            Assert.fail("The script shouldn't have been able to use the EntityManager.");
        } catch (ScriptException e) {
            checkIsDesiredSecurityException(e);
        }   
    }
    
    public void testRemoteJNDILookupWorksFromScripts() throws Exception {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();        
        
        ScriptEngine engine = getEngine(overlord);

        try {
            engine.eval(""
                + "var env = new java.util.Hashtable();"
                + "env.put('java.naming.factory.initial', 'org.jboss.naming.NamingContextFactory');"
                + "env.put('java.naming.provider.url', 'jnp://localhost:" + jnpPort + "');"
                + "var ctx = new javax.naming.InitialContext(env);\n"
                + "var kachny = ctx.lookup('kachny');\n"
                + "assertNotNull(kachny);\n");
        } catch (ScriptException e) {            
            Assert.fail("The script should have been able to access a remote JNDI server.", e);
        }   
    }
        
    private ScriptEngine getEngine(Subject subject) throws ScriptException, IOException {
        StandardBindings bindings = new StandardBindings(new PrintWriter(System.out), new LocalClient(subject));
        return ScriptEngineFactory.getSecuredScriptEngine("javascript", new PackageFinder(Collections.<File>emptyList()), bindings, new StandardScriptPermissions());
    }
    
    private static void checkIsDesiredSecurityException(ScriptException e) {
        String message = e.getMessage();
        String permissionTrace = AllowRhqServerInternalsAccessPermission.class.getName();
        
        Assert.assertTrue(message.contains(permissionTrace), "The script exception doesn't seem to be caused by the AllowRhqServerInternalsAccessPermission security exception. " + message);
    }    
}
