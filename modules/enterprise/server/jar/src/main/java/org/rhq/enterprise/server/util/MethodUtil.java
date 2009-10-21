package org.rhq.enterprise.server.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple class that assists with method invocation.  We should just use 
 * the jakarta-commons MethodUtils class, but that class can't deal with 
 * static methods, so it is useless to us.
 * @version $Rev$
 */
public class MethodUtil {

    private static final Log log = LogFactory.getLog(MethodUtil.class);

    /**
     * Private constructore
     */
    private MethodUtil() {
    }

    /* This is insanity itself, but the reflection APIs ignore inheritance.
     * So, if you ask for a method that accepts (Integer, HashMap, HashMap),
     * and the class only has (Integer, Map, Map), you won't find the method.
     * This method uses Class.isAssignableFrom to solve this problem.
     */
    private static boolean isCompatible(Class[] declaredParams, Object[] params) {
        if (params.length != declaredParams.length) {
            return false;
        }

        for (int i = 0; i < params.length; i++) {
            if (!declaredParams[i].isInstance(params[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Return the classname from the config.  Useful if you want to configure a different
     * class to be returned in specific instances.
     * @param className to check for overridden value. 
     * @return className from config or the className from parameter if not found
     */
    public static Object getClassFromSystemProperty(String className, Object... args) {
        if (System.getProperties().containsKey(className)) {
            String overridename = System.getProperty(className);
            return callNewMethod(overridename, args);
        }
        return callNewMethod(className, args);
    }

    /**
     * Create a new instance of the classname passed in.
     * 
     * @param className
     * @return instance of class passed in.
     */
    private static Object callNewMethod(String className, Object... args) {
        Object retval = null;

        try {
            Class clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            if (args == null || args.length == 0) {
                retval = clazz.newInstance();
            } else {
                try {
                    Constructor[] ctors = clazz.getConstructors();
                    for (Constructor ctor : ctors) {
                        if (isCompatible(ctor.getParameterTypes(), args)) {
                            return ctor.newInstance(args);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }

        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return retval;
    }
}
