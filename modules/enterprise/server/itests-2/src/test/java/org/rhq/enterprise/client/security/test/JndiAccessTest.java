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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PermissionCollection;
import java.util.Collections;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.bindings.ScriptEngineFactory;
import org.rhq.bindings.StandardBindings;
import org.rhq.bindings.StandardScriptPermissions;
import org.rhq.bindings.util.PackageFinder;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.LocalClient;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerBean;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test(enabled = false)
// TODO JNDI: reenable after fixing secure jndi lookups
public class JndiAccessTest extends AbstractEJB3Test {

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
        
        String jndiName = "java:global/rhq/rhq-enterprise-server-ejb3/" + SubjectManagerBean.class.getSimpleName()
            + "!" + SubjectManagerBean.class.getName().replace("Bean", "Local");

        try {
            engine.eval(""
                + "var ctx = new javax.naming.InitialContext();\n"
                + "var subjectManager = ctx.lookup('" + jndiName + "');\n"
                + "subjectManager.getOverlord();");
            
            Assert.fail("The script shouldn't have been able to call local SLSB method.");
        } catch (ScriptException e) {
            checkIsDesiredSecurityException(e);
        }
    }
    
    public void testRemoteEjbsInaccessibleThroughJndiLookup() throws ScriptException, IOException {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        
        ScriptEngine engine = getEngine(overlord);

        String jndiName = "java:global/rhq/rhq-enterprise-server-ejb3/" + SubjectManagerBean.class.getSimpleName()
            + "!" + SubjectManagerBean.class.getName().replace("Bean", "Remote");

        try {
            engine.eval(""
                + "var ctx = new javax.naming.InitialContext();\n"
                + "var subjectManager = ctx.lookup('" + jndiName + "');\n"
                + "subjectManager.getSubjectByName('rhqadmin');");
            
            Assert.fail("The script shouldn't have been able to call remote SLSB method directly.");
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
                    Assert.fail("The script shouldn't have been able to call a method on a SessionManager: " + methodCall);
                } catch (ScriptException e) {
                    checkIsDesiredSecurityException(e);
                }
            }
        };
        G manager = new G();
        
        manager.testInvoke("getlastAccess(0);");
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
            
            Assert.fail("The script shouldn't have been able to obtain the datasource from the JNDI.");
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
            
            Assert.fail("The script shouldn't have been able to use the EntityManager.");
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
            
            Assert.fail("The script shouldn't have been able to use the EntityManager even using custom initial context factory.");
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
        
    private ScriptEngine getEngine(Subject subject) throws ScriptException, IOException {
        StandardBindings bindings = new StandardBindings(new PrintWriter(System.out), new LocalClient(subject));
        
        PermissionCollection perms = new StandardScriptPermissions();
        
        return ScriptEngineFactory.getSecuredScriptEngine("javascript",
            new PackageFinder(Collections.<File> emptyList()), bindings, perms);
    }
    
    private static void checkIsDesiredSecurityException(ScriptException e) {
        String message = e.getMessage();
        String permissionTrace = null; // TODO JNDI: AllowRhqServerInternalsAccessPermission.class.getName();
        
        Assert.assertTrue(message.contains(permissionTrace), "The script exception doesn't seem to be caused by the AllowRhqServerInternalsAccessPermission security exception. " + message);
    }

    private static void checkIsNotASecurityException(ScriptException e) {
        String message = e.getMessage();
        String permissionTrace = null; // TODO JNDI: AllowRhqServerInternalsAccessPermission.class.getName();
        
        Assert.assertFalse(message.contains(permissionTrace), "The script exception does seem to be caused by the AllowRhqServerInternalsAccessPermission security exception although it shouldn't. " + message);
    }
}
