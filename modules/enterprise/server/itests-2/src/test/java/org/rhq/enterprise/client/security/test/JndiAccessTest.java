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
package org.rhq.enterprise.client.security.test;

import java.io.IOException;

import javax.naming.Context;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.ScriptableAbstractEJB3Test;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.system.SystemManagerBean;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 *
 *
 * @author Lukas Krejci
 */
@Test
public class JndiAccessTest extends ScriptableAbstractEJB3Test {

    private static final boolean SECURITY_MANAGER_IS_ENABLED = (System.getProperty("java.security.manager") != null);

    private static void failIfSecurityManagerEnabled(String msg) {
        failIfSecurityManagerEnabled(msg, null);
    }

    private static void failIfSecurityManagerEnabled(String msg, Throwable t) {
        if (SECURITY_MANAGER_IS_ENABLED) {
            Assert.fail(msg, t);
        } else {
            System.out.println("This test would have failed, but the security manager is disabled so it will pass: "
                + msg);
            if (t != null) {
                t.printStackTrace();
            }
        }
    }

    public void testScriptCantOverrideSystemProperties() throws Exception {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        ScriptEngine engine = getEngine(overlord);

        try {
            engine.eval("java.lang.System.setProperty('java.naming.factory.url.pkgs', 'counterfeit');");
        } catch (ScriptException e) {
            Assert.assertTrue(
                e.getMessage().contains(
                    "access denied (\"java.util.PropertyPermission\" \"java.naming.factory.url.pkgs\" \"write\")"),
                "The script shouldn't have write access to the system properties.");
        }
    }

    public void testEjbsAccessibleThroughPrivilegedCode() {
        LookupUtil.getSubjectManager().getOverlord();
    }

    public void testEjbsAccessibleThroughLocalClient() throws ScriptException, IOException {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        ScriptEngine engine = getEngine(overlord);

        engine.eval("SubjectManager.getSubjectByName('rhqadmin');");
    }

    public void testLocalEjbsInaccessibleThroughJndiLookup() throws ScriptException, IOException {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        ScriptEngine engine = getEngine(overlord);

        String jndiName = "java:global/rhq/rhq-server/"
            + SystemManagerBean.class.getSimpleName()
            + "!"
            + SystemManagerBean.class.getName().replace("Bean", "Local");

        try {
            engine.eval(""
                + "var ctx = new javax.naming.InitialContext();\n"
                + "var systemManager = ctx.lookup('"
                + jndiName
                + "');\n"
                + "systemManager.isDebugModeEnabled();");

            failIfSecurityManagerEnabled("The script shouldn't have been able to call local SLSB method.");
        } catch (ScriptException e) {
            checkIsDesiredSecurityException(e);
        }
    }

    public void testLocalEjbsInaccessibleThroughJndiLookupWithCustomUrlPackages() throws ScriptException, IOException {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        ScriptEngine engine = getEngine(overlord);

        String jndiName = "java:global/rhq/rhq-server/"
            + SystemManagerBean.class.getSimpleName()
            + "!"
            + SystemManagerBean.class.getName().replace("Bean", "Local");

        try {
            engine.eval(""
                + "var env = new java.util.Hashtable();\n"
                + "env.put('"
                + Context.URL_PKG_PREFIXES
                + "', 'org.jboss.as.naming.interfaces');\n"
                + "var ctx = new javax.naming.InitialContext(env);\n"
                + "var systemManager = ctx.lookup('"
                + jndiName
                + "');\n"
                + "systemManager.isDebugModeEnabled();");

            failIfSecurityManagerEnabled("The script shouldn't have been able to call local SLSB method.");
        } catch (ScriptException e) {
            checkIsDesiredSecurityException(e);
        }
    }

    public void testRemoteEjbsInaccessibleThroughJndiLookup() throws ScriptException, IOException {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        ScriptEngine engine = getEngine(overlord);

        String jndiName = "java:global/rhq/rhq-server/"
            + SystemManagerBean.class.getSimpleName()
            + "!"
            + SystemManagerBean.class.getName().replace("Bean", "Remote");

        try {
            engine.eval(""
                + "var ctx = new javax.naming.InitialContext();\n"
                + "var systemManager = ctx.lookup('"
                + jndiName
                + "');\n"
                + "systemManager.getSystemSettings(subject);");

            failIfSecurityManagerEnabled("The script shouldn't have been able to call remote SLSB method directly.");
        } catch (ScriptException e) {
            checkIsDesiredSecurityException(e);
        }
    }

    public void testScriptCantUseSessionManagerMethods() throws Exception {

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        final ScriptEngine engine = getEngine(overlord);

        class G {
            private String sessionManager = ""
                + "org.rhq.enterprise.server.auth.SessionManager.getInstance().";

            public void testInvoke(String methodCall) throws ScriptException {
                String code = sessionManager + methodCall;

                try {
                    engine.eval(code);
                    failIfSecurityManagerEnabled("The script shouldn't have been able to call a method on a SessionManager: "
                        + methodCall);
                } catch (ScriptException e) {
                    checkIsDesiredSecurityException(e);
                }
            }
        };
        G manager = new G();

        manager.testInvoke("getLastAccess(0);");
        manager.testInvoke("getOverlord()");
        manager.testInvoke("getSubject(2);");
        manager.testInvoke("invalidate(0);");
        manager.testInvoke("invalidate(\"\");");
        manager.testInvoke("put(new org.rhq.core.domain.auth.Subject());");
        manager.testInvoke("put(new org.rhq.core.domain.auth.Subject(), 0);");
    }

    public void testScriptCantObtainRawJDBCConnectionsWithoutCredentials() throws Exception {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        ScriptEngine engine = getEngine(overlord);

        try {
            engine.eval(""
                + "var ctx = new javax.naming.InitialContext();\n"
                + "var datasource = ctx.lookup('" + RHQConstants.DATASOURCE_JNDI_NAME + "');\n"
                + "con = datasource.getConnection();");

            failIfSecurityManagerEnabled("The script shouldn't have been able to obtain the datasource from the JNDI.");
        } catch (ScriptException e) {
            checkIsDesiredSecurityException(e);
        }
    }

    public void testScriptCantUseEntityManager() throws Exception {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        ScriptEngine engine = getEngine(overlord);

        try {
            engine.eval(""
                + "var ctx = new javax.naming.InitialContext();\n"
                + "var entityManagerFactory = ctx.lookup('" + RHQConstants.ENTITY_MANAGER_JNDI_NAME + "');\n"
                + "var entityManager = entityManagerFactory.createEntityManager();\n"
                + "entityManager.find(java.lang.Class.forName('org.rhq.core.domain.resource.Resource'), java.lang.Integer.valueOf('10001'));");

            failIfSecurityManagerEnabled("The script shouldn't have been able to use the EntityManager.");
        } catch (ScriptException e) {
            checkIsDesiredSecurityException(e);
        }

        //try harder with manually specifying the initial context factory
        try {
            engine.eval(""
                + "var env = new java.util.Hashtable();"
                + "env.put('java.naming.factory.initial', 'org.jboss.as.naming.InitialContextFactory');"
                //+ "env.put('java.naming.factory.url.pkgs', 'org.jboss.naming:org.jnp.interfaces');"
                + "var ctx = new javax.naming.InitialContext(env);\n"
                + "var entityManagerFactory = ctx.lookup('" + RHQConstants.ENTITY_MANAGER_JNDI_NAME + "');\n"
                + "var entityManager = entityManagerFactory.createEntityManager();\n"
                + "entityManager.find(java.lang.Class.forName('org.rhq.core.domain.resource.Resource'), java.lang.Integer.valueOf('10001'));");

            failIfSecurityManagerEnabled("The script shouldn't have been able to use the EntityManager even using custom initial context factory.");
        } catch (ScriptException e) {
            checkIsDesiredSecurityException(e);
        }
    }

    public void testProxyFactoryWorksWithSecuredScriptEngine() throws Exception {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();

        ScriptEngine engine = getEngine(overlord);

        try {
            engine.eval("var resource = ProxyFactory.getResource(10001);");
        } catch (ScriptException e) {
            //if the script fails (there is no resource with ID 10001)
            //it should not be because of an access control exception
            checkIsNotASecurityException(e);
        }
    }

    //THIS IS A NEW REQUIREMENT THAT DOESN'T CURRENTLY WORK...
    //    public void testInitialContextFactoryBuilderNotReplaceableUsingScripts() throws Exception {
    //        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
    //
    //        ScriptEngine engine = getEngine(overlord);
    //
    //        try {
    //            engine.eval(""
    //                + "var mgrCls = java.lang.Class.forName('javax.naming.spi.NamingManager');\n"
    //                + "var fld = mgrCls.getDeclaredField('initctx_factory_builder');\n"
    //                + "fld.setAccessible(true);\n"
    //                + "fld.set(null, null);\n");
    //
    //            Assert.fail("It should not be possible to use reflection to reset the initial context factory builder.");
    //        } catch (ScriptException e) {
    //            checkIsDesiredSecurityException(e);
    //        } finally {
    //            //restore the default behavior
    //            NamingHack.bruteForceInitialContextFactoryBuilder();
    //        }
    //    }

    private static void checkIsDesiredSecurityException(ScriptException e) {
        String message = e.getMessage();
        String permissionTrace = "org.rhq.allow.server.internals.access";

        if (!message.contains(permissionTrace)) {
            failIfSecurityManagerEnabled(
                "The script exception doesn't seem to be caused by the AllowRhqServerInternalsAccessPermission security exception.",
                e);
        }
    }

    private static void checkIsNotASecurityException(ScriptException e) {
        String message = e.getMessage();
        String permissionTrace = "org.rhq.allow.server.internals.access";

        if (message.contains(permissionTrace)) {
            Assert
                .fail(
                    "The script exception does seem to be caused by the AllowRhqServerInternalsAccessPermission security exception although it shouldn't. ",
                e);
        }
    }
}
