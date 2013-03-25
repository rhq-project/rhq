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

package org.rhq.test.arquillian.impl.util;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

import org.rhq.test.arquillian.impl.RhqAgentPluginContainer;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class PluginContainerClassEnhancer {

    private static boolean initialized;
    
    //let's be brutal - PluginContainer is a singleton, which doesn't really play nice with
    //Arquillian if we want to test with multiple differently configured containers.
    //So let's make some modifications to to PluginContainer code to make it possible
    //to have multiple instances of PC around.
    //Stuff is going to work as long as no two PC instances are going to be used concurrently.
    //This for example means that the scheduled scanners in the PC are going to break
    //if they ever kick in while 2 PC instances are running.
    //The default configuration of PC in Arquillian is to never run scanners on schedule but
    //only on demand (see RhqAgentPluginContainerConfiguration class).
    public static void init() {
        if (!initialized) {
            try {
                ClassLoader cl = RhqAgentPluginContainer.class.getClassLoader();
                CtClass pcClass = ClassPool.getDefault().get("org.rhq.core.pc.PluginContainer");
                CtField instancesField = CtField.make("private static java.util.Map INSTANCES = new java.util.HashMap();", pcClass);
                pcClass.addField(instancesField);
                
                CtField currentInstanceField = CtField.make("private static String CURRENT_INSTANCE_NAME;", pcClass);
                pcClass.addField(currentInstanceField);
                
                CtMethod getInstance = pcClass.getMethod("getInstance", "()Lorg/rhq/core/pc/PluginContainer;");
                getInstance.setBody("return (org.rhq.core.pc.PluginContainer) INSTANCES.get(CURRENT_INSTANCE_NAME);");
                
                CtMethod setContainerInstance = CtMethod.make("public static void setContainerInstance(java.lang.String name) { CURRENT_INSTANCE_NAME = name; if (!INSTANCES.containsKey((Object)name)) { INSTANCES.put(name, new org.rhq.core.pc.PluginContainer());}}", pcClass);
                pcClass.addMethod(setContainerInstance);
                
                CtMethod createContainerInstance = CtMethod.make("public static org.rhq.core.pc.PluginContainer getContainerInstance(java.lang.String name) { org.rhq.core.pc.PluginContainer inst = (org.rhq.core.pc.PluginContainer) INSTANCES.get(name); if (inst == null) { inst = new org.rhq.core.pc.PluginContainer(); INSTANCES.put(name, inst);} return inst;}", pcClass);
                pcClass.addMethod(createContainerInstance);
                
                pcClass.toClass(cl, null);
            } catch (Exception e) {
                throw new IllegalStateException("Could not enhance the PluginContainer class", e);
            } finally {
                initialized = true;
            }
        }
    }
    

}
