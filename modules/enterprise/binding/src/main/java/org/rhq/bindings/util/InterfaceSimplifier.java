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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.ParameterAnnotationsAttribute;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ByteMemberValue;
import javassist.bytecode.annotation.CharMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.DoubleMemberValue;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.FloatMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.MemberValueVisitor;
import javassist.bytecode.annotation.ShortMemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import org.apache.commons.logging.Log;

import org.rhq.bindings.security.SecurityActions;
import org.rhq.core.domain.auth.Subject;

/**
 * The scripts can use simplified interfaces that omit the first "Subject" argument
 * to most methods from RHQ's remote API. This helper class prepares such simplified interfaces.
 * 
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public class InterfaceSimplifier {
    private static final Log LOG = SecurityActions.getLog(InterfaceSimplifier.class);

    private InterfaceSimplifier() {

    }

    /**
     * Returns the method on the original interface that the simplified interface with given method was generated from
     * using the {@link #simplify(Class)} method (i.e. this method is kind of reverse to the {@link #simplify(Class)} 
     * method).
     * <p>
     * The returned method may or may not have different signature from the supplied method - that depends on whether
     * the {@link #simplify(Class)} simplified the method or not.
     *  
     * @param method the potentially simplified method
     * @return null if the method doesn't come from a simplified class, otherwise a method on the original interface
     * that the supplied method was generated from.
     */
    public static Method getOriginalMethod(Method method) {
        SimplifiedClass simplifiedClass = method.getDeclaringClass().getAnnotation(SimplifiedClass.class);
        if (simplifiedClass == null) {
            return null;
        } else {
            SimplifiedMethod simplifiedMethod = method.getAnnotation(SimplifiedMethod.class);
            Class<?> origClass = simplifiedClass.originalClass();

            if (simplifiedMethod == null) {
                try {
                    return origClass.getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException("Inconsisten interface simplification. The non-simplified method "
                        + method + " should have had a counterpart with the exact signature on the interface "
                        + origClass + " but it could not be found.", e);
                }
            } else {

                Class<?>[] paramTypes = new Class<?>[method.getParameterTypes().length + 1];
                paramTypes[0] = Subject.class;
                System.arraycopy(method.getParameterTypes(), 0, paramTypes, 1, paramTypes.length - 1);

                try {
                    return origClass.getMethod(method.getName(), paramTypes);
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException("Inconsistent interface simplification. The simplified method "
                        + method + " should have had a counterpart on the interface " + origClass
                        + " but it couldn't be found.", e);
                }
            }
        }
    }

    /**
     * Determines whether given class is simplified or not. This method will return true for any class returned from 
     * {@link #simplify(Class)}.
     * 
     * @param cls the class
     * @return true if the class object was created by the {@link #simplify(Class)} method, false otherwise.
     */
    public static boolean isSimplified(Class<?> cls) {
        return cls.getAnnotation(SimplifiedClass.class) != null;
    }

    /**
     * Determines whether the method (declared on the simplified interface, i.e. 
     * <code>isSimplified(method.getDeclaringClass()</code> returns true) has been "tampered with" by the simplifier or
     * has been left intact.
     * <p>
     * If you want to get the original method that the supplied method corresponds to, use 
     * the {@link #getOriginalMethod(Method)} method.
     * 
     * @param method the potentially simplified method present on a simplified class
     * @return true if the method's signature has been modified by the simplifier, false otherwise.
     */
    public static boolean isSimplified(Method method) {
        return method.getAnnotation(SimplifiedMethod.class) != null;
    }

    public static Class<?> simplify(Class<?> intf) {
        try {
            ClassPool classPool = ClassPool.getDefault();

            String simplifiedName = getSimplifiedName(intf);
            LOG.debug("Simplifying " + intf + " (simplified interface name: " + simplifiedName + ")...");

            CtClass cached = null;
            try {
                cached = classPool.get(simplifiedName);
                return Class.forName(simplifiedName, false, classPool.getClassLoader());
            } catch (NotFoundException e) {
                // ok... load it
            } catch (ClassNotFoundException e) {
                LOG.debug("Class [" + simplifiedName + "] not found - cause: " + e, e);
                if (cached != null) {
                    // strange - we found the class definition in the class pool, which means we must have touched it 
                    // before but Class.forName failed to find the class in the class pool's class loader.
                    return cached.toClass();
                }
            }

            CtClass originalClass = classPool.get(intf.getName());
            ClassFile originalClassFile = originalClass.getClassFile();

            CtClass newClass = classPool.makeInterface(simplifiedName);

            newClass.defrost();

            ClassFile newClassFile = newClass.getClassFile();

            //we'll be adding new constants to the class file (for generics and annotations)
            ConstPool constPool = newClassFile.getConstPool();

            //copy the annotations on the class
            AnnotationsAttribute annotations = (AnnotationsAttribute) originalClassFile
                .getAttribute(AnnotationsAttribute.visibleTag);
            AnnotationsAttribute newAnnotations = copyAnnotations(annotations, constPool);

            //add our @Simplified annotation to the new class
            newAnnotations = addSimplifiedClassAnnotation(originalClass.getName(), newAnnotations, constPool);
            newClassFile.addAttribute(newAnnotations);

            //copy the generic signature of the class
            SignatureAttribute signature = (SignatureAttribute) originalClassFile.getAttribute(SignatureAttribute.tag);
            if (signature != null) {
                newClassFile.addAttribute(new SignatureAttribute(constPool, signature.getSignature()));
            }

            //now copy over the methods
            CtMethod[] methods = originalClass.getMethods();

            for (CtMethod originalMethod : methods) {

                //we are only simplifying interfaces here, but the CtClass.getMethods() also returns concrete methods
                //inherited from Object. Let's just skip those - we don't need to worry about them...
                if (!Modifier.isAbstract(originalMethod.getModifiers())) {
                    continue;
                }

                CtClass[] params = originalMethod.getParameterTypes();

                //capture all the runtime visible method annotations on the original method
                annotations = (AnnotationsAttribute) originalMethod.getMethodInfo().getAttribute(
                    AnnotationsAttribute.visibleTag);

                //capture all the runtime visible parameter annotations on the original method
                ParameterAnnotationsAttribute parameterAnnotations = (ParameterAnnotationsAttribute) originalMethod
                    .getMethodInfo().getAttribute(ParameterAnnotationsAttribute.visibleTag);

                //capture the generic signature of the original method.
                signature = (SignatureAttribute) originalMethod.getMethodInfo().getAttribute(
                    SignatureAttribute.tag);

                boolean simplify = params.length > 0 && params[0].getName().equals(Subject.class.getName());

                if (simplify) {
                    //generate new params, leaving out the first parameter (the subject)
                    CtClass[] simpleParams = new CtClass[params.length - 1];

                    System.arraycopy(params, 1, simpleParams, 0, params.length - 1);
                    params = simpleParams;
                }

                //generate the new method with possibly modified parameters
                CtMethod newMethod = CtNewMethod.abstractMethod(originalMethod.getReturnType(),
                    originalMethod.getName(), params, originalMethod.getExceptionTypes(), newClass);

                //copy over the method annotations
                annotations = copyAnnotations(annotations, constPool);

                if (simplify) {
                    //add the @SimplifiedMethod to the method annotations
                    annotations = addSimplifiedMethodAnnotation(annotations, constPool);

                    if (signature != null) {
                        //fun, we need to modify the signature, too, because we have left out the parameter
                        MethodSignature sig = MethodSignature.parse(signature.getSignature());

                        sig.paramTypes.remove(0);

                        signature = new SignatureAttribute(constPool, sig.toString());
                    }

                    //next, we need to copy the parameter annotations
                    parameterAnnotations = copyParameterAnnotations(parameterAnnotations, constPool, 1);
                } else {
                    //just copy the sig and parameter annotations verbatim
                    if (signature != null) {
                        signature = new SignatureAttribute(constPool, signature.getSignature());
                    }

                    parameterAnnotations = copyParameterAnnotations(parameterAnnotations, constPool, 0);
                }

                if (parameterAnnotations != null) {
                    newMethod.getMethodInfo().addAttribute(parameterAnnotations);
                }

                if (signature != null) {
                    newMethod.getMethodInfo().addAttribute(signature);
                }

                if (annotations != null) {
                    newMethod.getMethodInfo().addAttribute(annotations);
                }

                //it is important to add the method directly to the classfile, not the class
                //because otherwise the generics info wouldn't survive
                newClassFile.addMethod(newMethod.getMethodInfo());
            }

            return newClass.toClass();

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
        String packageName = (pkg != null) ? pkg.getName() : fullName.substring(0,
            fullName.length() - (simpleName.length() + 1));
        return packageName + ".wrapped." + simpleName + "Simple";
    }

    /**
     * Copies the provided annotation into the provided const pool.
     * 
     * @param annotation
     * @param constPool
     * @return
     * @throws NotFoundException
     */
    private static Annotation cloneAnnotation(Annotation annotation, final ConstPool constPool)
        throws NotFoundException {

        Annotation ret = new Annotation(annotation.getTypeName(), constPool);

        if (annotation.getMemberNames() != null) {
            for (Object m : annotation.getMemberNames()) {
                final String memberName = (String) m;

                MemberValue origValue = annotation.getMemberValue(memberName);
                final MemberValue[] newValue = new MemberValue[1];

                origValue.accept(new ArrayIndexAssigningVisitor(newValue, 0, constPool));

                ret.addMemberValue(memberName, newValue[0]);
            }
        }

        return ret;
    }

    private static AnnotationsAttribute copyAnnotations(AnnotationsAttribute annotations, ConstPool constPool)
        throws NotFoundException {
        if (annotations != null) {
            Annotation[] origAnnotations = annotations.getAnnotations();
            Annotation[] newClassAnnotations = new Annotation[origAnnotations.length];
            for (int i = 0; i < newClassAnnotations.length; ++i) {
                newClassAnnotations[i] = cloneAnnotation(origAnnotations[i], constPool);
            }

            AnnotationsAttribute newAnnotations = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
            newAnnotations.setAnnotations(newClassAnnotations);

            return newAnnotations;
        }

        return null;
    }

    private static ParameterAnnotationsAttribute copyParameterAnnotations(
        ParameterAnnotationsAttribute parameterAnnotations, ConstPool constPool, int fromIndex)
        throws NotFoundException {

        if (parameterAnnotations != null) {
            Annotation[][] originalAnnotations = parameterAnnotations.getAnnotations();

            //return early if there are no annotations to copy
            if (originalAnnotations.length - fromIndex <= 0) {
                return null;
            }

            Annotation[][] newParameterAnnotations = new Annotation[originalAnnotations.length - fromIndex][];

            for (int i = fromIndex; i < originalAnnotations.length; i++) {
                newParameterAnnotations[i - fromIndex] = new Annotation[originalAnnotations[i].length];
                for (int j = 0; j < originalAnnotations[i].length; ++j) {
                    Annotation origAnnotation = originalAnnotations[i][j];

                    newParameterAnnotations[i - fromIndex][j] = cloneAnnotation(origAnnotation, constPool);
                }
            }

            ParameterAnnotationsAttribute newAnnotationsAttribute = new ParameterAnnotationsAttribute(constPool,
                ParameterAnnotationsAttribute.visibleTag);

            newAnnotationsAttribute.setAnnotations(newParameterAnnotations);

            return newAnnotationsAttribute;
        }

        return null;
    }

    private static class ArrayIndexAssigningVisitor implements MemberValueVisitor {
        private MemberValue[] array;
        private int index;
        private ConstPool constPool;

        public ArrayIndexAssigningVisitor(MemberValue[] array, int index, ConstPool constPool) {
            this.array = array;
            this.index = index;
            this.constPool = constPool;
        }

        @Override
        public void visitStringMemberValue(StringMemberValue node) {
            array[index] = new StringMemberValue(node.getValue(), constPool);
        }

        @Override
        public void visitShortMemberValue(ShortMemberValue node) {
            array[index] = new ShortMemberValue(node.getValue(), constPool);
        }

        @Override
        public void visitLongMemberValue(LongMemberValue node) {
            array[index] = new LongMemberValue(node.getValue(), constPool);
        }

        @Override
        public void visitIntegerMemberValue(IntegerMemberValue node) {
            array[index] = new IntegerMemberValue(constPool, node.getValue());
        }

        @Override
        public void visitFloatMemberValue(FloatMemberValue node) {
            array[index] = new FloatMemberValue(node.getValue(), constPool);
        }

        @Override
        public void visitEnumMemberValue(EnumMemberValue node) {
            EnumMemberValue val = new EnumMemberValue(constPool);
            val.setType(node.getType());
            val.setValue(node.getValue());
            array[index] = val;
        }

        @Override
        public void visitDoubleMemberValue(DoubleMemberValue node) {
            array[index] = new DoubleMemberValue(node.getValue(), constPool);
        }

        @Override
        public void visitClassMemberValue(ClassMemberValue node) {
            array[index] = new ClassMemberValue(node.getValue(), constPool);
        }

        @Override
        public void visitCharMemberValue(CharMemberValue node) {
            array[index] = new CharMemberValue(node.getValue(), constPool);
        }

        @Override
        public void visitByteMemberValue(ByteMemberValue node) {
            array[index] = new ByteMemberValue(node.getValue(), constPool);
        }

        @Override
        public void visitBooleanMemberValue(BooleanMemberValue node) {
            array[index] = new BooleanMemberValue(node.getValue(), constPool);
        }

        @Override
        public void visitArrayMemberValue(ArrayMemberValue node) {
            ArrayMemberValue val = new ArrayMemberValue(node.getType(), constPool);
            MemberValue[] newVals = new MemberValue[node.getValue().length];
            for (int i = 0; i < node.getValue().length; ++i) {
                node.getValue()[i].accept(new ArrayIndexAssigningVisitor(newVals, i, constPool));
            }

            val.setValue(newVals);
            array[index] = val;
        }

        @Override
        public void visitAnnotationMemberValue(AnnotationMemberValue node) {
            array[index] = new AnnotationMemberValue(node.getValue(), constPool);
        }
    }

    //a quick and dirty method signature parser
    //see http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.4
    private static class MethodSignature {
        public String returnType;
        public List<String> paramTypes = new ArrayList<String>();
        public String typeParameters;
        public String exceptionTypes;
        
        public static MethodSignature parse(String signature) {
            int startParams = signature.indexOf('(') + 1;
            int endParams = signature.indexOf(')');
            int startExceptions = signature.indexOf('^');

            MethodSignature sig = new MethodSignature();
            sig.typeParameters = signature.substring(0, startParams - 1);
            if (startExceptions == -1) {
                sig.returnType = signature.substring(endParams + 1);
                sig.exceptionTypes = "";
            } else {
                sig.returnType = signature.substring(endParams + 1, startExceptions);
                sig.exceptionTypes = signature.substring(startExceptions);
            }

            int idx = startParams;
            while (idx < endParams) {
                int end = findEndOfTypeSignature(idx, signature);
                sig.paramTypes.add(signature.substring(idx, end));
                idx = end;
            }

            return sig;
        }

        private static int findEndOfTypeSignature(int idx, String signature) {
            int c = signature.charAt(idx);

            switch (c) {
            case 'L':
                return findEndOfClassSignature(idx, signature);
            case '[':
                return findEndOfTypeSignature(idx + 1, signature);
            case 'T':
                return signature.indexOf(';', idx + 1) + 1;
            default:
                return idx + 1;
            }
        }

        private static int findEndOfClassSignature(int indexOfL, String signature) {
            int idx = indexOfL + 1;

            int genericDeclDepth = 0;

            while (idx < signature.length()) {
                boolean sigComplete = false;

                char c = signature.charAt(idx++);
                switch (c) {
                case '<':
                    genericDeclDepth++;
                    break;
                case '>':
                    genericDeclDepth--;
                    break;
                case ';':
                    sigComplete = genericDeclDepth == 0;
                    break;
                }

                if (sigComplete) {
                    break;
                }
            }

            return idx;
        }

        @Override
        public String toString() {
            StringBuilder bld = new StringBuilder(typeParameters);
            bld.append("(");
            for (String p : paramTypes) {
                bld.append(p);
            }
            bld.append(")");
            bld.append(returnType);
            bld.append(exceptionTypes);

            return bld.toString();
        }
    }

    private static AnnotationsAttribute addSimplifiedClassAnnotation(String originalClassName,
        AnnotationsAttribute annotations, ConstPool constPool) {

        if (annotations == null) {
            annotations = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        }

        Annotation simplified = new Annotation(SimplifiedClass.class.getName(), constPool);
        simplified.addMemberValue("originalClass", new ClassMemberValue(originalClassName, constPool));

        annotations.addAnnotation(simplified);

        return annotations;
    }

    private static AnnotationsAttribute addSimplifiedMethodAnnotation(AnnotationsAttribute annotations,
        ConstPool constPool) {

        if (annotations == null) {
            annotations = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        }

        annotations.addAnnotation(new Annotation(SimplifiedMethod.class.getName(), constPool));

        return annotations;
    }
}
