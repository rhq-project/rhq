/*
 * JBoss, a division of Red Hat.
 * Copyright 2007, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.enterprise.server.safeinvoker;

import org.jboss.aspects.remoting.AOPRemotingInvocationHandler;
import org.jboss.remoting.InvocationRequest;
import org.jboss.remoting.invocation.NameBasedInvocation;

import javax.naming.InitialContext;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.HashMap;

import org.rhq.enterprise.server.util.HibernateDetachUtility;

/**
 * This rather hackish endpoint is here to handle remote invocations over the standard
 * invocation handler.
 *
 * @author Greg Hinkle
 */
public class RemoteSafeAOPInvocationHandler extends AOPRemotingInvocationHandler {


    public static final Map<String, Class> PRIMITIVE_CLASSES;

    static {
        PRIMITIVE_CLASSES = new HashMap<String, Class>();
        PRIMITIVE_CLASSES.put(Short.TYPE.getName(), Short.TYPE);
        PRIMITIVE_CLASSES.put(Integer.TYPE.getName(), Integer.TYPE);
        PRIMITIVE_CLASSES.put(Long.TYPE.getName(), Long.TYPE);
        PRIMITIVE_CLASSES.put(Float.TYPE.getName(), Float.TYPE);
        PRIMITIVE_CLASSES.put(Double.TYPE.getName(), Double.TYPE);
        PRIMITIVE_CLASSES.put(Boolean.TYPE.getName(), Boolean.TYPE);
        PRIMITIVE_CLASSES.put(Character.TYPE.getName(), Character.TYPE);
        PRIMITIVE_CLASSES.put(Byte.TYPE.getName(), Byte.TYPE);
    }


    public Object invoke(InvocationRequest invocationRequest) throws Throwable {

        Object result = null;
        long time = System.currentTimeMillis();
        try {
            InitialContext ic = new InitialContext();

            NameBasedInvocation nbi = ((NameBasedInvocation) invocationRequest.getParameter());
            String[] methodInfo = nbi.getMethodName().split(":");

            String jndiName = "rhq/" + methodInfo[0] + "/local";
            Object target = ic.lookup(jndiName);

            Class[] sig = new Class[nbi.getSignature().length];
            int i = 0;
            for (String cn : nbi.getSignature()) {
                sig[i] = getClass(nbi.getSignature()[i++]);
            }

            Method m = target.getClass().getMethod(methodInfo[1], sig);

            result = m.invoke(target, nbi.getParameters());


        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return e.getTargetException();
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        } finally {

            if (result != null) {

                try {
                    HibernateDetachUtility.nullOutUninitializedFields(result, HibernateDetachUtility.SerializationType.SERIALIZATION);
                } catch (Exception e) {
                    e.printStackTrace();
                    return e;
                }
//                   System.out.println("ExecutionTime: " + (System.currentTimeMillis() - time));

            }
        }

        return result;
    }

    Class getClass(String name) throws ClassNotFoundException {
        // TODO GH: Doesn't support arrays
        if (PRIMITIVE_CLASSES.containsKey(name)) {
            return PRIMITIVE_CLASSES.get(name);
        } else {
            return Class.forName(name);
        }

    }
}