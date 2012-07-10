/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;

/**
 * @author Lukas Krejci
 */
@Test
public class InterfaceSimplifierTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.PARAMETER, ElementType.METHOD })
    public static @interface MyAnnotation {
        int value() default 1;

        String parameter();
    }

    public static interface NoSimplifications {
        void voidMethodWithNoExceptions();

        void voidMethodWithExceptions() throws IOException, InterruptedException;

        int intMethod();

        Object objectMethod();

        Object objectMethodWithParams(int p1, Object p2);

        Object objectMethodWithParamsAndExceptions(int p1, Object p2) throws IOException, InterruptedException;
    }

    public static interface Simplifications {
        void voidMethodWithNoExceptions(Subject s);

        void voidMethodWithExceptions(Subject s) throws IOException, InterruptedException;

        int intMethod(Subject s);

        Object objectMethod(Subject s);

        Object objectMethodWithParams(Subject s, int p1, Object p2);

        Object objectMethodWithParamsAndExceptions(Subject s, int p1, Object p2) throws IOException,
            InterruptedException;
    }

    public static interface Generics<C extends Type> {
        void genericParameters(List<String> p, int p2);

        <T extends Type> T typeParameters(T p, int p2);

        <T extends Type> T typeParametersSimplified(Subject s, T p, int p2);

        <T extends C> T classTypeParameters(T p, int p2);
    }


    @MyAnnotation(parameter = "CLASS")
    public static interface Annotations {

        @MyAnnotation(value = 2, parameter = "a")
        int method(@MyAnnotation(parameter = "b") int p) throws IOException;

        @MyAnnotation(value = 2, parameter = "c")
        int methodSimplified(@MyAnnotation(parameter = "disappear") Subject s, @MyAnnotation(parameter = "d") int p)
            throws IOException;
    }

    public void testNoSimplifications() throws Exception {
        Class<?> iface = InterfaceSimplifier.simplify(NoSimplifications.class);

        Method voidMethodWithNoExceptions = iface.getMethod("voidMethodWithNoExceptions");
        Assert.assertEquals(voidMethodWithNoExceptions.getReturnType(), void.class);
        
        Method voidMethodWithExceptions = iface.getMethod("voidMethodWithExceptions");
        List<Class<?>> exceptions = Arrays.asList(voidMethodWithExceptions.getExceptionTypes());
        Assert.assertTrue(exceptions.contains(IOException.class),
            "The 'voidMethodWithExceptions doesn't seem to declare throws IOException");
        Assert.assertTrue(exceptions.contains(InterruptedException.class),
            "The 'voidMethodWithExceptions doesn't seem to declare throws InterruptedException");
        
        Method intMethod = iface.getMethod("intMethod");
        Assert.assertEquals(intMethod.getReturnType(), int.class);

        Method objectMethod = iface.getMethod("objectMethod");
        Assert.assertEquals(objectMethod.getReturnType(), Object.class);

        Method objectMethodWithParams = iface.getMethod("objectMethodWithParams", int.class, Object.class);
        Assert.assertEquals(objectMethodWithParams.getReturnType(), Object.class);

        Method objectMethodWithParamsAndExceptions = iface.getMethod("objectMethodWithParamsAndExceptions", int.class,
            Object.class);
        Assert.assertEquals(objectMethodWithParamsAndExceptions.getReturnType(), Object.class);
        exceptions = Arrays.asList(objectMethodWithParamsAndExceptions.getExceptionTypes());
        Assert.assertTrue(exceptions.contains(IOException.class),
            "The 'objectMethodWithParamsAndExceptions doesn't seem to declare throws IOException");
        Assert.assertTrue(exceptions.contains(InterruptedException.class),
            "The 'objectMethodWithParamsAndExceptions doesn't seem to declare throws InterruptedException");
    }

    public void testSimplifications() throws Exception {
        Class<?> iface = InterfaceSimplifier.simplify(Simplifications.class);

        //These tests are exactly the same as for the NoSimplifications class, because
        //the simplifier should leave out all the subject parameters in the Simplifications
        //class' methods.

        Method voidMethodWithNoExceptions = iface.getMethod("voidMethodWithNoExceptions");
        Assert.assertEquals(voidMethodWithNoExceptions.getReturnType(), void.class);

        Method voidMethodWithExceptions = iface.getMethod("voidMethodWithExceptions");
        List<Class<?>> exceptions = Arrays.asList(voidMethodWithExceptions.getExceptionTypes());
        Assert.assertTrue(exceptions.contains(IOException.class),
            "The 'voidMethodWithExceptions doesn't seem to declare throws IOException");
        Assert.assertTrue(exceptions.contains(InterruptedException.class),
            "The 'voidMethodWithExceptions doesn't seem to declare throws InterruptedException");

        Method intMethod = iface.getMethod("intMethod");
        Assert.assertEquals(intMethod.getReturnType(), int.class);

        Method objectMethod = iface.getMethod("objectMethod");
        Assert.assertEquals(objectMethod.getReturnType(), Object.class);

        Method objectMethodWithParams = iface.getMethod("objectMethodWithParams", int.class, Object.class);
        Assert.assertEquals(objectMethodWithParams.getReturnType(), Object.class);

        Method objectMethodWithParamsAndExceptions = iface.getMethod("objectMethodWithParamsAndExceptions", int.class,
            Object.class);
        Assert.assertEquals(objectMethodWithParamsAndExceptions.getReturnType(), Object.class);
        exceptions = Arrays.asList(objectMethodWithParamsAndExceptions.getExceptionTypes());
        Assert.assertTrue(exceptions.contains(IOException.class),
            "The 'objectMethodWithParamsAndExceptions doesn't seem to declare throws IOException");
        Assert.assertTrue(exceptions.contains(InterruptedException.class),
            "The 'objectMethodWithParamsAndExceptions doesn't seem to declare throws InterruptedException");
    }

    public <T> void testGenerics() throws Exception {
        @SuppressWarnings("unchecked")
        Class<T> iface = (Class<T>) InterfaceSimplifier.simplify(Generics.class);

        TypeVariable<Class<T>>[] classTypeParameters = iface.getTypeParameters();
        Assert.assertEquals(classTypeParameters.length, 1, "There should be 1 type parameter on the class Generics.");
        TypeVariable<?> typeVariable = classTypeParameters[0];
        Assert.assertEquals(typeVariable.getName(), "C", "Unexpected type parameter name on 'Generics' class.");
        Type[] bounds = typeVariable.getBounds();
        Assert.assertEquals(bounds.length, 1, "The type parameter on the class 'Generics' should have 1 upper bound.");
        Assert.assertEquals(bounds[0], Type.class,
            "The type parameter on the class 'Generics' should have the upper bound of the Type class.");
        
        Method genericParameters = iface.getMethod("genericParameters", List.class, int.class);
        Assert.assertEquals(genericParameters.getReturnType(), void.class);
        
        Type firstParamType = genericParameters.getGenericParameterTypes()[0];
        Assert.assertTrue(firstParamType instanceof ParameterizedType,
            "The first parameter of the 'genericParameters' should be parameterized.");
        Assert.assertEquals(((ParameterizedType) firstParamType).getRawType(), List.class,
            "The first parameter of the 'genericParameters' method should be a List.");
        Assert.assertEquals(((ParameterizedType) firstParamType).getActualTypeArguments()[0], String.class,
            "The first parameter of the 'genericParamters' method should be a List<String>");

        Method typeParameters = iface.getMethod("typeParameters", Type.class, int.class);
        Assert.assertEquals(typeParameters.getReturnType(), Type.class);

        TypeVariable<Method>[] typeVariables = typeParameters.getTypeParameters();
        Assert.assertEquals(typeVariables.length, 1,
            "There should be 1 type parameter on the the 'typeParameters' method.");
        typeVariable = typeVariables[0];
        Assert.assertEquals(typeVariable.getName(), "T", "Unexpected type parameter name on 'typeParameters' method.");
        bounds = typeVariable.getBounds();
        Assert.assertEquals(bounds.length, 1, "The type parameter on the method 'typeParameters' should have 1 upper bound.");
        Assert.assertEquals(bounds[0], Type.class,
            "The type parameter on the method 'typeParameters' should have the upper bound of the Type class.");

        Type returnType = typeParameters.getGenericReturnType();
        Assert.assertTrue(returnType instanceof TypeVariable,
            "The generic return type of the 'typeParameters' class should be a type variable.");
        typeVariable = (TypeVariable<?>) returnType;
        Assert.assertEquals(typeVariable.getName(), "T",
            "Unexpected type parameter at the return type of the 'typeParameters' method.");

        Method typeParametersSimplified = iface.getMethod("typeParametersSimplified", Type.class, int.class);
        Assert.assertEquals(typeParameters.getReturnType(), Type.class);

        typeVariables = typeParametersSimplified.getTypeParameters();
        Assert.assertEquals(typeVariables.length, 1,
            "There should be 1 type parameter on the the 'typeParametersSimplified' method.");
        typeVariable = typeVariables[0];
        Assert.assertEquals(typeVariable.getName(), "T",
            "Unexpected type parameter name on 'typeParametersSimplified' method.");
        bounds = typeVariable.getBounds();
        Assert.assertEquals(bounds.length, 1,
            "The type parameter on the method 'typeParametersSimplified' should have 1 upper bound.");
        Assert
            .assertEquals(bounds[0], Type.class,
                "The type parameter on the method 'typeParametersSimplified' should have the upper bound of the Type class.");

        returnType = typeParametersSimplified.getGenericReturnType();
        Assert.assertTrue(returnType instanceof TypeVariable,
            "The generic return type of the 'typeParametersSimplified' class should be a type variable.");
        typeVariable = (TypeVariable<?>) returnType;
        Assert.assertEquals(typeVariable.getName(), "T",
            "Unexpected type parameter at the return type of the 'typeParametersSimplified' method.");
    }
    
    public void testAnnotations() throws Exception {
        Class<?> iface = InterfaceSimplifier.simplify(Annotations.class);

        Annotation[] annotations = iface.getAnnotations();
        //we add the @SimplifiedClass annotation
        Assert.assertEquals(annotations.length, Annotations.class.getAnnotations().length + 1,
            "Unexpected number of annotations on the 'Annotations' class.");
        Annotation annotation = annotations[0];
        Assert.assertEquals(annotation.annotationType(), MyAnnotation.class,
            "Unexpected annotation type on the class 'Annotations");
        Assert.assertEquals(((MyAnnotation) annotation).value(), 1,
            "Unexpected value of the 'value' attribute on the annotation on the 'Annotations' class.");
        Assert.assertEquals(((MyAnnotation) annotation).parameter(), "CLASS",
            "Unexpected value of the 'parameter' attribute on the annotation on the 'Annotations' class.");

        Method method = iface.getMethod("method", int.class);
        annotations = method.getAnnotations();
        Assert.assertEquals(annotations.length, 1, "Unexpected number of annotations on the 'method' method.");

        annotation = annotations[0];
        Assert.assertEquals(annotation.annotationType(), MyAnnotation.class,
            "Unexpected annotation type on the method 'method");

        Assert.assertEquals(((MyAnnotation) annotation).value(), 2,
            "Unexpected value of the 'value' attribute on the annotation on the 'method' method.");
        Assert.assertEquals(((MyAnnotation) annotation).parameter(), "a",
            "Unexpected value of the 'parameter' attribute on the annotation on the 'method' method.");

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        Assert
            .assertEquals(parameterAnnotations.length, 1,
                "Method 'Annotations.method(int)' has 1 parameter with annotations but we got a different number of parameters.");

        Assert.assertEquals(parameterAnnotations[0].length, 1,
            "The parameter of 'Annotations.method(int)' method has an annotation but we couldn't detect any.");

        annotation = parameterAnnotations[0][0];
        Assert.assertEquals(annotation.annotationType(), MyAnnotation.class,
            "Unexpected annotation type on the parameter 'p' of 'Annotations.method(int)'");
        Assert.assertEquals(((MyAnnotation) annotation).value(), 1,
            "Unexpected value of the 'value' of the annotation on the parameter p of 'Annotations.method(int)'.");
        Assert.assertEquals(((MyAnnotation) annotation).parameter(), "b",
            "Unexpected value of the 'parameter' of the annotation on the parameter p of 'Annotations.method(int)'.");

        method = iface.getMethod("methodSimplified", int.class);
        annotations = method.getAnnotations();
        //we add the @SimplifiedMethod on the method
        Assert
            .assertEquals(annotations.length, 2, "Unexpected number of annotations on the 'methodSimplified' method.");

        annotation = annotations[0];
        Assert.assertEquals(annotation.annotationType(), MyAnnotation.class,
            "Unexpected annotation type on the method 'methodSimplified");

        Assert.assertEquals(((MyAnnotation) annotation).value(), 2,
            "Unexpected value of the 'value' attribute on the annotation on the 'methodSimplified' method.");
        Assert.assertEquals(((MyAnnotation) annotation).parameter(), "c",
            "Unexpected value of the 'parameter' attribute on the annotation on the 'methodSimplified' method.");

        parameterAnnotations = method.getParameterAnnotations();

        Assert
            .assertEquals(parameterAnnotations.length, 1,
                "Method 'Annotations.methodSimplified(int)' has 1 parameter with annotations but we got a different number of parameters.");

        Assert.assertEquals(parameterAnnotations[0].length, 1,
                "The parameter of 'Annotations.methodSimplified(int)' method has an annotation but we couldn't detect any.");

        annotation = parameterAnnotations[0][0];
        Assert.assertEquals(annotation.annotationType(), MyAnnotation.class,
            "Unexpected annotation type on the parameter 'p' of 'Annotations.methodSimplified(int)'");
        Assert.assertEquals(((MyAnnotation) annotation).value(), 1,
                "Unexpected value of the 'value' of the annotation on the parameter p of 'Annotations.methodSimplified(int)'.");
        Assert
            .assertEquals(((MyAnnotation) annotation).parameter(), "d",
                "Unexpected value of the 'parameter' of the annotation on the parameter p of 'Annotations.methodSimplified(int)'.");
    }

    public void testOriginalMethodRetrieval() throws Exception {
        Class<?> iface = InterfaceSimplifier.simplify(Generics.class);

        Method simplifiedMethod = iface.getMethod("typeParametersSimplified", Type.class, int.class);
        Method origMethod = InterfaceSimplifier.getOriginalMethod(simplifiedMethod);

        Assert.assertTrue(InterfaceSimplifier.isSimplified(iface),
            "Unable to determine that the simplified interface was simplified.");
        Assert.assertTrue(InterfaceSimplifier.isSimplified(simplifiedMethod));
        Assert.assertFalse(InterfaceSimplifier.isSimplified(String.class), "String class is NOT simplified.");
        Assert.assertFalse(InterfaceSimplifier.isSimplified(Object.class.getMethod("toString")),
            "Object.toString() is NOT simplified.");

        Assert.assertEquals(origMethod.getDeclaringClass(), Generics.class,
            "Unexpected declaring class of the original method.");
        Assert.assertEquals(origMethod.getParameterTypes().length, simplifiedMethod.getParameterTypes().length + 1,
            "Unexpected number of params on the original method.");
        Assert.assertEquals(origMethod.getParameterTypes()[0], Subject.class,
            "Unexpected first param of the original method.");

        Method nonSimplifiedMethod = iface.getMethod("typeParameters", Type.class, int.class);
        origMethod = InterfaceSimplifier.getOriginalMethod(nonSimplifiedMethod);

        Assert.assertFalse(InterfaceSimplifier.isSimplified(nonSimplifiedMethod));

        Assert.assertEquals(origMethod.getDeclaringClass(), Generics.class,
            "Unexpected declaring class of the original method.");
        Assert.assertEquals(origMethod.getParameterTypes(), nonSimplifiedMethod.getParameterTypes(),
            "Unexpected params on the original method.");
    }
}
