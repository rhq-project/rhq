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
package org.rhq.bindings.util;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.NotFoundException;
import javassist.bytecode.ParameterAnnotationsAttribute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;

/**
 * The scripts can use simplified interfaces that omit the first "Subject" argument
 * to most methods. This helper class prepares such simplified interfaces.
 * 
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public class InterfaceSimplifier {
    private static final Log LOG = LogFactory.getLog(InterfaceSimplifier.class);
    
    private InterfaceSimplifier() {
        
    }
    
    public static Class<?> simplify(Class<?> intf) {
        try {
            ClassPool cp = ClassPool.getDefault();

            String simplifiedName = getSimplifiedName(intf);
            LOG.debug("Simplifying " + intf + " (simplified interface name: " + simplifiedName + ")...");

            try {
                CtClass cached = cp.get(simplifiedName);
                return Class.forName(simplifiedName, false, cp.getClassLoader());

            } catch (NotFoundException e) {
                // ok... load it
            } catch (ClassNotFoundException e) {
                LOG.debug("Class [" + simplifiedName + "] not found - cause: " + e);
            }

            CtClass cc = cp.get(intf.getName());

            CtClass cz = cp.getAndRename(intf.getName(), simplifiedName);
            //            CtClass cz = cp.makeInterface(simpleName, cc);

            cz.defrost();

            cz.setSuperclass(cc);

            CtMethod[] methods = cc.getMethods();

            for (CtMethod originalMethod : methods) {

                CtClass[] params = originalMethod.getParameterTypes();
                if (params.length > 0 && params[0].getName().equals(Subject.class.getName())) {

                    CtClass[] simpleParams = new CtClass[params.length - 1];

                    System.arraycopy(params, 1, simpleParams, 0, params.length - 1);
                    cz.defrost();

                    CtMethod newMethod = CtNewMethod.abstractMethod(originalMethod.getReturnType(), originalMethod
                        .getName(), simpleParams, null, cz);

                    ParameterAnnotationsAttribute originalAnnotationsAttribute = (ParameterAnnotationsAttribute) originalMethod
                        .getMethodInfo().getAttribute(ParameterAnnotationsAttribute.visibleTag);

                    // If there are any parameter annotations, copy the one's we're keeping
                    if (originalAnnotationsAttribute != null) {

                        javassist.bytecode.annotation.Annotation[][] originalAnnotations = originalAnnotationsAttribute
                            .getAnnotations();
                        javassist.bytecode.annotation.Annotation[][] newAnnotations = new javassist.bytecode.annotation.Annotation[originalAnnotations.length - 1][];

                        for (int i = 1; i < originalAnnotations.length; i++) {
                            newAnnotations[i - 1] = new javassist.bytecode.annotation.Annotation[originalAnnotations[i].length];
                            System.arraycopy(originalAnnotations[i], 0, newAnnotations[i - 1], 0,
                                originalAnnotations[i].length);
                        }

                        ParameterAnnotationsAttribute newAnnotationsAttribute = new ParameterAnnotationsAttribute(
                            newMethod.getMethodInfo().getConstPool(), ParameterAnnotationsAttribute.visibleTag);

                        newAnnotationsAttribute.setAnnotations(newAnnotations);

                        newMethod.getMethodInfo().addAttribute(newAnnotationsAttribute);

                    }

                    cz.addMethod(newMethod);
                }
            }

            return cz.toClass();

        } catch (NotFoundException e) {
            LOG.debug("Failed to simplify " + intf + " - cause: " + e);
        } catch (CannotCompileException e) {
            LOG.error("Failed to simplify " + intf + ".", e);
        }
        return intf;
    }

    private static String getSimplifiedName(Class<?> interfaceClass) {
        String fullName = interfaceClass.getName();
        String simpleName = interfaceClass.getSimpleName();
        Package pkg = interfaceClass.getPackage();
        String packageName = (pkg != null) ? pkg.getName() :
                fullName.substring(0, fullName.length() - (simpleName.length() + 1));
        return packageName + ".wrapped." + simpleName + "Simple";
    }

}
