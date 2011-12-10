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
import java.io.SerializablePermission;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.PermissionCollection;
import java.util.Collections;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.bindings.SandboxedScriptEngine;
import org.rhq.bindings.ScriptEngineFactory;
import org.rhq.bindings.StandardBindings;
import org.rhq.bindings.StandardScriptPermissions;
import org.rhq.bindings.util.PackageFinder;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.client.LocalClient;
import org.rhq.enterprise.server.security.AllowEjbAccessPermission;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class EjbAccessTest extends AbstractEJB3Test {

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
        
        try {
            engine.eval(""
                + "context = new javax.naming.InitialContext();\n"
                + "subjectManager = context.lookup('SubjectManagerBean/local');\n"
                + "subjectManager.getOverlord();");
            
            Assert.fail("The script shouldn't have been able to call local SLSB method.");
        } catch (ScriptException e) {
            assert e.getMessage().contains(AllowEjbAccessPermission.class.getName());
        }
    }
    
    public void testRemoteEjbsInaccessibleThroughJndiLookup() throws ScriptException, IOException {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        
        ScriptEngine engine = getEngine(overlord);
        
        try {
            engine.eval(""
                + "context = new javax.naming.InitialContext();\n"
                + "subjectManager = context.lookup('SubjectManagerBean/remote');\n"
                + "subjectManager.getSubjectByName('rhqadmin');");
            
            Assert.fail("The script shouldn't have been able to call remote SLSB method directly.");
        } catch (ScriptException e) {
            //TODO java.io.IOException: access denied (java.io.SerializablePermission enableSubclassImplementation)
            assert e.getMessage().contains(AllowEjbAccessPermission.class.getName());
        }
    }
    
    public void testScriptCantUseSessionManager() throws ScriptException, IOException {
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        
        ScriptEngine engine = getEngine(overlord);
        
        try {
            engine.eval("org.rhq.enterprise.server.auth.SessionManager.getInstance();");
            
            Assert.fail("The script shouldn't have been able to get instance of SessionManager.");
        } catch (ScriptException e) {
            assert e.getMessage().contains(AllowEjbAccessPermission.class.getName());
        }
    }

    @SuppressWarnings("unused")
    public void testScriptCantUseSessionManagerMethods() throws Exception {

        //The code below cannot work in Rhino because as of now, Rhino modifies
        //the return value of Class.getDeclaredField() to be null when there
        //is a security manager installed.
        //This has been filed as a bug at Oracle (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7119959)
        
        if (true) {
            return;
        }
        
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();        
        
        final ScriptEngine engine = getEngine(overlord);
            
        class G {
            private String obtainSessionManagerUsingReflection = ""
                + "var sessionManagerClass = java.lang.Class.forName(\"org.rhq.enterprise.server.auth.SessionManager\");\n"
                + "println(sessionManagerClass);\n"
                + "var managerField = sessionManagerClass.getDeclaredField(\"_manager\");\n"
                + "println(managerField);\n"
                + "managerField.setAccessible(true);\n"
                + "var manager = managerField.get(null);\n"
                + "println(manager);\n"
                + "manager.";
            
            public void testInvoke(String methodCall) throws ScriptException {
                String code = obtainSessionManagerUsingReflection + methodCall;

                try {
                    engine.eval(code);               
                    Assert.fail("The script shouldn't have been able to a method on a SessionManager.");
                } catch (ScriptException e) {
                    assert e.getMessage().contains(AllowEjbAccessPermission.class.getName());
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
    
    private ScriptEngine getEngine(Subject subject) throws ScriptException, IOException {
        StandardBindings bindings = new StandardBindings(new PrintWriter(System.out), new LocalClient(subject));
        ScriptEngine engine = ScriptEngineFactory.getScriptEngine("JavaScript", new PackageFinder(Collections.<File>emptyList()), bindings);
        
        PermissionCollection perms = new StandardScriptPermissions();
        perms.add(new SerializablePermission("enableSubclassImplementation"));
        
        return new SandboxedScriptEngine(engine, perms);
    }
}
