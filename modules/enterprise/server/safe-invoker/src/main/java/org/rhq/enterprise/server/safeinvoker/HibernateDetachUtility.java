/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.safeinvoker;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

/**
 * This is a single static utility that is used to process any object just prior to sending it over the wire
 * to remote clients (like GWT clients or remote web service clients).
 *
 * Essentially this utility scrubs the object of all Hibernate proxies, cleaning it such that it
 * can be serialized over the wire successfully.
 *
 * @author Greg Hinkle
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public class HibernateDetachUtility {

    private static final Log LOG = LogFactory.getLog(HibernateDetachUtility.class);

    public static enum SerializationType {
        SERIALIZATION, JAXB
    }

    /*
     * This is the type of object that will be used to generate identity
     * hashcodes for objects that are being scanned during the detach.
     * During production runtime, the hashCodeGenerator will use the
     * java.lang.System mechanism, but since this is package scoped,
     * tests can override this since. (See BZ 732089).
     */
    static interface HashCodeGenerator {
        Integer getHashCode(Object value);
    }

    static class SystemHashCodeGenerator implements HashCodeGenerator {
        @Override
        public Integer getHashCode(Object value) {
            return System.identityHashCode(value);
        }
    }

    static HashCodeGenerator hashCodeGenerator = new SystemHashCodeGenerator();

    // be able to configure the deepest recursion this utility will be allowed to go (see BZ 702109 that precipitated this need)
    private static final String DEPTH_ALLOWED_SYSPROP = "rhq.server.hibernate-detach-utility.depth-allowed";
    private static final String THROW_EXCEPTION_ON_DEPTH_LIMIT_SYSPROP = "rhq.server.hibernate-detach-utility.throw-exception-on-depth-limit";
    private static final int depthAllowed;
    private static final boolean throwExceptionOnDepthLimit;
    private static final String DUMP_STACK_THRESHOLDS_SYSPROP = "rhq.server.hibernate-detach-utility.dump-stack-thresholds"; // e.g. "5000:10000" (millis:num-objects)
    private static final boolean dumpStackOnThresholdLimit;
    private static final long millisThresholdLimit;
    private static final int sizeThresholdLimit;

    static {
        int value;
        try {
            String str = System.getProperty(DEPTH_ALLOWED_SYSPROP, "100");
            value = Integer.parseInt(str);
        } catch (Throwable t) {
            value = 100;
        }
        depthAllowed = value;

        boolean booleanValue;
        try {
            String str = System.getProperty(THROW_EXCEPTION_ON_DEPTH_LIMIT_SYSPROP, "true");
            booleanValue = Boolean.parseBoolean(str);
        } catch (Throwable t) {
            booleanValue = true;
        }
        throwExceptionOnDepthLimit = booleanValue;

        boolean tmp_dumpStackOnThresholdLimit;
        long tmp_millisThresholdLimit;
        int tmp_sizeThresholdLimit;
        String prop = null;
        try {
            prop = System.getProperty(DUMP_STACK_THRESHOLDS_SYSPROP);
            if (prop != null) {
                String[] nums = prop.split(":");
                tmp_dumpStackOnThresholdLimit = true;
                tmp_millisThresholdLimit = Long.parseLong(nums[0]);
                tmp_sizeThresholdLimit = Integer.parseInt(nums[1]);
            } else {
                tmp_dumpStackOnThresholdLimit = false;
                tmp_millisThresholdLimit = Long.MAX_VALUE;
                tmp_sizeThresholdLimit = Integer.MAX_VALUE;
            }
        } catch (Throwable t) {
            LOG.warn("Bad value for [" + DUMP_STACK_THRESHOLDS_SYSPROP + "]=[" + prop + "]: " + t.toString());
            tmp_dumpStackOnThresholdLimit = true; // they wanted to set it to something, so give them some defaults
            tmp_millisThresholdLimit = 5000L; // 5 seconds
            tmp_sizeThresholdLimit = 10000; // 10K objects
        }
        dumpStackOnThresholdLimit = tmp_dumpStackOnThresholdLimit;
        millisThresholdLimit = tmp_millisThresholdLimit;
        sizeThresholdLimit = tmp_sizeThresholdLimit;
    }

    public static void nullOutUninitializedFields(Object value, SerializationType serializationType) throws Exception {
        long start = System.currentTimeMillis();
        Map<Integer, Object> checkedObjectMap = new HashMap<Integer, Object>();
        Map<Integer, List<Object>> checkedObjectCollisionMap = new HashMap<Integer, List<Object>>();
        nullOutUninitializedFields(value, checkedObjectMap, checkedObjectCollisionMap, 0, serializationType);
        long duration = System.currentTimeMillis() - start;

        if (dumpStackOnThresholdLimit) {
            int numObjectsProcessed = checkedObjectMap.size();
            if (duration > millisThresholdLimit || numObjectsProcessed > sizeThresholdLimit) {
                String rootObjectString = (value != null) ? value.getClass().toString() : "null";
                LOG.warn("Detached [" + numObjectsProcessed + "] objects in [" + duration + "]ms from root object ["
                    + rootObjectString + "]", new Throwable("HIBERNATE DETACH UTILITY STACK TRACE"));
            }
        } else {
            // 10s is really long, log SOMETHING
            if (duration > 10000L && LOG.isDebugEnabled()) {
                LOG.debug("Detached [" + checkedObjectMap.size() + "] objects in [" + duration + "]ms");
            }
        }

        // help the garbage collector be clearing these before we leave
        checkedObjectMap.clear();
        checkedObjectCollisionMap.clear();
    }

    /**
     * @param value the object needing to be detached/scrubbed.
     * @param checkedObjectMap This maps identityHashCodes to Objects we've already detached. In that way we can
     * quickly determine if we've already done the work for the incoming value and avoid taversing it again. This
     * works well almost all of the time, but it is possible that two different objects can have the same identity hash
     * (conflicts are always possible with a hash). In that case we utilize the checkedObjectCollisionMap (see below).
     * @param checkedObjectCollisionMap checkedObjectMap maps the identityhash to the *first* object with that hash. In
     * most cases there will only be mapping for one hash, but it is possible to encounter the same hash for multiple
     * objects, especially on 32bit or IBM JVMs. It is important to know if an object has already been detached
     * because if it is somehow self-referencing, we have to stop the recursion. This map holds the 2nd..Nth mapping
     * for a single hash and is used to ensure we never try to detach an object already processed.
     * @param depth used to stop infinite recursion, defaults to a depth we don't expectto see, but it is configurable.
     * @param serializationType
     * @throws Exception if a problem occurs
     * @throws IllegalStateException if the recursion depth limit is reached
     */
    private static void nullOutUninitializedFields(Object value, Map<Integer, Object> checkedObjectMap,
        Map<Integer, List<Object>> checkedObjectCollisionMap, int depth, SerializationType serializationType)
        throws Exception {
        if (depth > depthAllowed) {
            String warningMessage = "Recursed too deep [" + depth + " > " + depthAllowed
                + "], will not attempt to detach object of type ["
                + ((value != null) ? value.getClass().getName() : "N/A")
                + "]. This may cause serialization errors later. "
                + "You can try to work around this by setting the system property [" + DEPTH_ALLOWED_SYSPROP
                + "] to a value higher than [" + depth + "] or you can set the system property ["
                + THROW_EXCEPTION_ON_DEPTH_LIMIT_SYSPROP + "] to 'false'";
            LOG.warn(warningMessage);
            if (throwExceptionOnDepthLimit) {
                throw new IllegalStateException(warningMessage);
            }
            return;
        }

        if (null == value) {
            return;
        }

        // System.identityHashCode is a hash code, and therefore not guaranteed to be unique. And we've seen this
        // be the case.  So, we use it to try and avoid duplicating work, but handle the case when two objects may
        // have an identity crisis.
        Integer valueIdentity = hashCodeGenerator.getHashCode(value);
        Object checkedObject = checkedObjectMap.get(valueIdentity);

        if (null == checkedObject) {
            // if we have not yet encountered an object with this hash, store it in our map and start scrubbing
            checkedObjectMap.put(valueIdentity, value);

        } else if (value == checkedObject) {
            // if we have scrubbed this already, no more work to be done
            return;

        } else {
            // we have a situation where multiple objects have the same identity hashcode, work with our
            // collision map to decide whether it needs to be scrubbed and add if necessary.
            // Note that this code block is infrequently hit, it is by design that we've pushed the extra
            // work, map, etc, involved for this infrequent case into its own block. The standard cases must
            // be as fast and lean as possible.

            boolean alreadyDetached = false;
            List<Object> collisionObjects = checkedObjectCollisionMap.get(valueIdentity);

            if (null == collisionObjects) {
                // if this is the 2nd occurrence for this hash, create a new map entry
                collisionObjects = new ArrayList<Object>(1);
                checkedObjectCollisionMap.put(valueIdentity, collisionObjects);

            } else {
                // if we have scrubbed this already, no more work to be done
                for (Object collisionObject : collisionObjects) {
                    if (value == collisionObject) {
                        alreadyDetached = true;
                        break;
                    }
                }
            }

            if (LOG.isDebugEnabled()) {
                StringBuilder message = new StringBuilder("\n\tIDENTITY HASHCODE COLLISION [hash=");
                message.append(valueIdentity);
                message.append(", alreadyDetached=");
                message.append(alreadyDetached);
                message.append("]");
                message.append("\n\tCurrent  : ");
                message.append(value.getClass().getName());
                message.append("\n\t    ");
                message.append(value);
                message.append("\n\tPrevious : ");
                message.append(checkedObject.getClass().getName());
                message.append("\n\t    ");
                message.append(checkedObject);
                for (Object collisionObject : collisionObjects) {
                    message.append("\n\tPrevious : ");
                    message.append(collisionObject.getClass().getName());
                    message.append("\n\t    ");
                    message.append(collisionObject);
                }

                LOG.debug(message);
            }

            // now that we've done our logging, if already detached we're done. Otherwise add to the list of collision
            // objects for this hash, and start scrubbing
            if (alreadyDetached) {
                return;
            }

            collisionObjects.add(value);
        }

        // Perform the detaching
        if (value instanceof Object[]) {
            Object[] objArray = (Object[]) value;
            for (int i = 0; i < objArray.length; i++) {
                Object listEntry = objArray[i];
                Object replaceEntry = replaceObject(listEntry);
                if (replaceEntry != null) {
                    objArray[i] = replaceEntry;
                }
                nullOutUninitializedFields(objArray[i], checkedObjectMap, checkedObjectCollisionMap, depth + 1,
                    serializationType);
            }
        } else if (value instanceof List) {
            // Null out any entries in initialized collections
            ListIterator i = ((List) value).listIterator();
            while (i.hasNext()) {
                Object val = i.next();
                Object replace = replaceObject(val);
                if (replace != null) {
                    val = replace;
                    i.set(replace);
                }
                nullOutUninitializedFields(val, checkedObjectMap, checkedObjectCollisionMap, depth + 1,
                    serializationType);
            }

        } else if (value instanceof Collection) {
            Collection collection = (Collection) value;
            Collection itemsToBeReplaced = new ArrayList();
            Collection replacementItems = new ArrayList();
            for (Object item : collection) {
                Object replacementItem = replaceObject(item);
                if (replacementItem != null) {
                    itemsToBeReplaced.add(item);
                    replacementItems.add(replacementItem);
                    item = replacementItem;
                }
                nullOutUninitializedFields(item, checkedObjectMap, checkedObjectCollisionMap, depth + 1,
                    serializationType);
            }
            collection.removeAll(itemsToBeReplaced);
            collection.addAll(replacementItems); // watch out! if this collection is a Set, HashMap$MapSet doesn't support addAll. See BZ 688000
        } else if (value instanceof Map) {
            Map originalMap = (Map) value;
            HashMap<Object, Object> replaceMap = new HashMap<Object, Object>();
            for (Iterator i = originalMap.keySet().iterator(); i.hasNext();) {
                // get original key and value - these might be hibernate proxies
                Object originalKey = i.next();
                Object originalKeyValue = originalMap.get(originalKey);

                // replace with non-hibernate classes, if appropriate (will be null otherwise)
                Object replaceKey = replaceObject(originalKey);
                Object replaceValue = replaceObject(originalKeyValue);

                // if either original key or original value was a hibernate proxy object, we have to
                // remove it from the original map, and remember the replacement objects for later
                if (replaceKey != null || replaceValue != null) {
                    Object newKey = (replaceKey != null) ? replaceKey : originalKey;
                    Object newValue = (replaceValue != null) ? replaceValue : originalKeyValue;
                    replaceMap.put(newKey, newValue);
                    i.remove();
                }
            }

            // all hibernate proxies have been removed, we need to replace them with their
            // non-proxy object representations that we got from replaceObject() calls
            originalMap.putAll(replaceMap);

            // now go through each item in the map and null out their internal fields
            for (Object key : originalMap.keySet()) {
                nullOutUninitializedFields(originalMap.get(key), checkedObjectMap, checkedObjectCollisionMap,
                    depth + 1, serializationType);
                nullOutUninitializedFields(key, checkedObjectMap, checkedObjectCollisionMap, depth + 1,
                    serializationType);
            }
        } else if (value instanceof Enum) {
            // don't need to detach enums, treat them as special objects
            return;
        }

        if (serializationType == SerializationType.JAXB) {
            XmlAccessorType at = value.getClass().getAnnotation(XmlAccessorType.class);
            if (at != null && at.value() == XmlAccessType.FIELD) {
                nullOutFieldsByFieldAccess(value, checkedObjectMap, checkedObjectCollisionMap, depth, serializationType);
            } else {
                nullOutFieldsByAccessors(value, checkedObjectMap, checkedObjectCollisionMap, depth, serializationType);
            }
        } else if (serializationType == SerializationType.SERIALIZATION) {
            nullOutFieldsByFieldAccess(value, checkedObjectMap, checkedObjectCollisionMap, depth, serializationType);
        }

    }

    private static void nullOutFieldsByFieldAccess(Object object, Map<Integer, Object> checkedObjects,
        Map<Integer, List<Object>> checkedObjectCollisionMap, int depth, SerializationType serializationType)
        throws Exception {

        Class tmpClass = object.getClass();
        List<Field> fieldsToClean = new ArrayList<Field>();
        while (tmpClass != null && tmpClass != Object.class) {
            Field[] declaredFields = tmpClass.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                // do not process static final or transient fields since they won't be serialized anyway
                int modifiers = declaredField.getModifiers();
                if (!((Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers)) || Modifier.isTransient(modifiers))) {
                    fieldsToClean.add(declaredField);
                }
            }
            tmpClass = tmpClass.getSuperclass();
        }

        nullOutFieldsByFieldAccess(object, fieldsToClean, checkedObjects, checkedObjectCollisionMap, depth,
            serializationType);
    }

    private static void nullOutFieldsByFieldAccess(Object object, List<Field> classFields,
        Map<Integer, Object> checkedObjects, Map<Integer, List<Object>> checkedObjectCollisionMap, int depth,
        SerializationType serializationType) throws Exception {

        boolean accessModifierFlag = false;
        for (Field field : classFields) {
            accessModifierFlag = false;
            if (!field.isAccessible()) {
                field.setAccessible(true);
                accessModifierFlag = true;
            }

            Object fieldValue = field.get(object);

            if (fieldValue instanceof HibernateProxy) {

                Object replacement = null;
                String assistClassName = fieldValue.getClass().getName();
                if (assistClassName.contains("jvst") || assistClassName.contains("EnhancerByCGLIB")) {

                    Class assistClass = fieldValue.getClass();
                    try {
                        Method m = assistClass.getMethod("writeReplace");
                        replacement = m.invoke(fieldValue);

                        String assistNameDelimiter = assistClassName.contains("jvst") ? "_$$_" : "$$";

                        assistClassName = assistClassName.substring(0, assistClassName.indexOf(assistNameDelimiter));
                        if (replacement != null && !replacement.getClass().getName().contains("hibernate")) {
                            nullOutUninitializedFields(replacement, checkedObjects, checkedObjectCollisionMap,
                                depth + 1, serializationType);

                            field.set(object, replacement);
                        } else {
                            replacement = null;
                        }
                    } catch (Exception e) {
                        LOG.error("Unable to write replace object " + fieldValue.getClass(), e);
                    }
                }

                if (replacement == null) {

                    String className = ((HibernateProxy) fieldValue).getHibernateLazyInitializer().getEntityName();

                    //see if there is a context classloader we should use instead of the current one.
                    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

                    Class clazz = contextClassLoader == null ? Class.forName(className) : Class.forName(className,
                        true, contextClassLoader);
                    Class[] constArgs = { Integer.class };
                    Constructor construct = null;

                    try {
                        construct = clazz.getConstructor(constArgs);
                        replacement = construct.newInstance((Integer) ((HibernateProxy) fieldValue)
                            .getHibernateLazyInitializer().getIdentifier());
                        field.set(object, replacement);
                    } catch (NoSuchMethodException nsme) {

                        try {
                            Field idField = clazz.getDeclaredField("id");
                            Constructor ct = clazz.getDeclaredConstructor();
                            ct.setAccessible(true);
                            replacement = ct.newInstance();
                            if (!idField.isAccessible()) {
                                idField.setAccessible(true);
                            }
                            idField.set(replacement, ((HibernateProxy) fieldValue)
                                .getHibernateLazyInitializer().getIdentifier());
                        } catch (Exception e) {
                            e.printStackTrace();
                            LOG.error("No id constructor and unable to set field id for base bean " + className, e);
                        }

                        field.set(object, replacement);
                    }
                }

            } else {
                if (fieldValue instanceof org.hibernate.collection.spi.PersistentCollection) {
                    // Replace hibernate specific collection types

                    if (!((org.hibernate.collection.spi.PersistentCollection) fieldValue).wasInitialized()) {
                        field.set(object, null);
                    } else {

                        Object replacement = null;
                        boolean needToNullOutFields = true; // needed for BZ 688000
                        if (fieldValue instanceof Map) {
                            replacement = new HashMap((Map) fieldValue);
                        } else if (fieldValue instanceof List) {
                            replacement = new ArrayList((List) fieldValue);
                        } else if (fieldValue instanceof Set) {
                            ArrayList l = new ArrayList((Set) fieldValue); // cannot recurse Sets, see BZ 688000
                            nullOutUninitializedFields(l, checkedObjects, checkedObjectCollisionMap, depth + 1,
                                serializationType);
                            replacement = new HashSet(l); // convert it back to a Set since that's the type of the real collection, see BZ 688000
                            needToNullOutFields = false;
                        } else if (fieldValue instanceof Collection) {
                            replacement = new ArrayList((Collection) fieldValue);
                        }
                        setField(object, field.getName(), replacement);

                        if (needToNullOutFields) {
                            nullOutUninitializedFields(replacement, checkedObjects, checkedObjectCollisionMap,
                                depth + 1, serializationType);
                        }
                    }

                } else {
                    if (fieldValue != null
                        && (fieldValue.getClass().getName().contains("org.rhq") || fieldValue instanceof Collection
                            || fieldValue instanceof Object[] || fieldValue instanceof Map))
                        nullOutUninitializedFields((fieldValue), checkedObjects, checkedObjectCollisionMap, depth + 1,
                            serializationType);
                }
            }
            if (accessModifierFlag) {
                field.setAccessible(false);
            }
        }

    }

    private static Object replaceObject(Object object) {
        Object replacement = null;

        if (object instanceof HibernateProxy) {
            if (object.getClass().getName().contains("jvst")) {
                Class assistClass = object.getClass();
                try {
                    Method m = assistClass.getMethod("writeReplace");
                    replacement = m.invoke(object);

                } catch (Exception e) {
                    LOG.error("Unable to write replace object " + object.getClass(), e);
                }
            }
        }
        return replacement;
    }

    private static void nullOutFieldsByAccessors(Object value, Map<Integer, Object> checkedObjects,
        Map<Integer, List<Object>> checkedObjectCollisionMap, int depth, SerializationType serializationType)
        throws Exception {
        // Null out any collections that aren't loaded
        BeanInfo bi = Introspector.getBeanInfo(value.getClass(), Object.class);

        PropertyDescriptor[] pds = bi.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            Object propertyValue = null;
            try {
                propertyValue = pd.getReadMethod().invoke(value);
            } catch (Throwable lie) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Couldn't load: " + pd.getName() + " off of " + value.getClass().getSimpleName(), lie);
                }
            }

            if (!Hibernate.isInitialized(propertyValue)) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Nulling out: " + pd.getName() + " off of " + value.getClass().getSimpleName());
                    }

                    Method writeMethod = pd.getWriteMethod();
                    if ((writeMethod != null) && (writeMethod.getAnnotation(XmlTransient.class) == null)) {
                        pd.getWriteMethod().invoke(value, new Object[] { null });
                    } else {
                        nullOutField(value, pd.getName());
                    }
                } catch (Exception lie) {
                    LOG.debug("Couldn't null out: " + pd.getName() + " off of " + value.getClass().getSimpleName()
                        + " trying field access", lie);
                    nullOutField(value, pd.getName());
                }
            } else {
                if ((propertyValue instanceof Collection)
                    || ((propertyValue != null) && propertyValue.getClass().getName().startsWith("org.rhq.core.domain"))) {
                    nullOutUninitializedFields(propertyValue, checkedObjects, checkedObjectCollisionMap, depth + 1,
                        serializationType);
                }
            }
        }
    }

    private static void setField(Object object, String fieldName, Object newValue) {
        try {
            Field f = object.getClass().getDeclaredField(fieldName);
            if (f != null) {
                // try to set the field this way
                f.setAccessible(true);
                f.set(object, newValue);
            }
        } catch (NoSuchFieldException e) {
            // ignore this
        } catch (IllegalAccessException e) {
            // ignore this
        }
    }

    private static void nullOutField(Object value, String fieldName) {
        try {
            Field f = value.getClass().getDeclaredField(fieldName);
            if (f != null) {
                // try to set the field this way
                f.setAccessible(true);
                f.set(value, null);
            }
        } catch (NoSuchFieldException e) {
            // ignore this
        } catch (IllegalAccessException e) {
            // ignore this
        }
    }
}
