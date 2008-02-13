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
package org.rhq.enterprise.communications.command.param;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * A class holding specific information about a set of parameters. It follows the Java Collection interface contract and
 * thus can be used anywhere a Collection can be used.
 *
 * <p>This collection follows both list and map connotations. It behaves like a list in that items that come out of it
 * are in the same order as when they were added to it. It behaves like a map in that duplicate elements are not allowed
 * and looking up elements have constant time performance that is analogous to <code>java.util.HashMap</code>.
 *
 * <p>This class is <b>not</b> thread-safe.</p>
 *
 * @author <a href="ccrouch@jboss.com">Charles Crouch</a>
 * @author <a href="mazz@jboss.com">John Mazzitelli</a>
 */
public class ParametersImpl implements Parameters {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(ParametersImpl.class);

    /**
     * The serialVersionUID. Be careful if you want to change this - parameters in a serialized form may be persisted.
     */
    private static final long serialVersionUID = 1L;

    /**
     * A map containing all the Parameter objects; keyed on {@link ParameterNameIndex}.
     */
    private Map<ParameterNameIndex, Parameter> m_parameters;

    /**
     * The index of the last parameter that was added to the collection.
     */
    private int m_lastIndex;

    /**
     * Constructor for {@link ParametersImpl} that initializes the object with no parameters.
     */
    public ParametersImpl() {
        m_parameters = new HashMap<ParameterNameIndex, Parameter>();
        m_lastIndex = -1;
    }

    /**
     * Copy-constructor for {@link ParametersImpl}.
     *
     * @param original the original to copy
     */
    public ParametersImpl(Parameters original) {
        this();

        if (original != null) {
            // don't just do a m_parameters.putAll - we want to create a duplicate of the Parameter object
            for (Iterator iter = original.iterator(); iter.hasNext();) {
                Parameter parameter = (Parameter) iter.next();
                add(new Parameter(parameter));
            }
        }

        return;
    }

    /**
     * @see Parameters#getParameter(String)
     */
    public Parameter getParameter(String parameterName) {
        if (parameterName == null) {
            return null;
        }

        return m_parameters.get(new ParameterNameIndex(parameterName, -1));
    }

    /**
     * @see Parameters#getParameterDefinition(String)
     */
    public ParameterDefinition getParameterDefinition(String parameterName) throws InvalidParameterDefinitionException {
        Parameter parameter = getParameter(parameterName);

        if (parameter == null) {
            throw new InvalidParameterDefinitionException(LOG.getMsgString(CommI18NResourceKeys.NO_PARAMETER,
                parameterName));
        }

        return parameter.getDefinition();
    }

    /**
     * @see Parameters#getParameterValue(String)
     */
    public Object getParameterValue(String parameterName) throws InvalidParameterDefinitionException {
        Parameter parameter = getParameter(parameterName);

        if (parameter == null) {
            throw new InvalidParameterDefinitionException(LOG.getMsgString(CommI18NResourceKeys.NO_PARAMETER,
                parameterName));
        }

        return parameter.getValue();
    }

    /**
     * @see Parameters#setParameterValue(String, Object)
     */
    public void setParameterValue(String parameterName, Object parameterValue)
        throws InvalidParameterDefinitionException {
        Parameter parameter = getParameter(parameterName);

        if (parameter == null) {
            throw new InvalidParameterDefinitionException(LOG.getMsgString(CommI18NResourceKeys.NO_PARAMETER,
                parameterName));
        }

        parameter.setValue(parameterValue);

        return;
    }

    /**
     * The returned object is not a copy - changes made to that collection affect this object's internal collection.
     * That means that if you add or remove parameters from the returned object, those parameters are also added or
     * removed from this object.
     *
     * @see Parameters#getPublicParameters()
     */
    public Parameters getPublicParameters() {
        return new Proxy(false);
    }

    /**
     * The returned object is not a copy - changes made to that collection affect this object's internal collection.
     * That means that if you add or remove parameters from the returned object, those parameters are also added or
     * removed from this object.
     *
     * @see Parameters#getInternalParameters()
     */
    public Parameters getInternalParameters() {
        return new Proxy(true);
    }

    /**
     * @see Parameters#applyResourceBundleToParameterRenderingInformation(ResourceBundle)
     */
    public void applyResourceBundleToParameterRenderingInformation(ResourceBundle resourceBundle) {
        for (Iterator iter = iterator(); iter.hasNext();) {
            Parameter param = (Parameter) iter.next();
            ParameterDefinition definition = param.getDefinition();
            ParameterRenderingInformation renderingInfo = definition.getRenderingInfo();

            renderingInfo.applyResourceBundle(resourceBundle);
        }
    }

    /**
     * The returned object is not a copy - changes made to that collection affect this object's internal collection.
     * That means that if you add or remove parameters from the returned object, those parameters are also added or
     * removed from this object.
     *
     * @see java.util.Collection#size()
     */
    public int size() {
        return m_parameters.size();
    }

    /**
     * @see java.util.Collection#clear()
     */
    public void clear() {
        m_parameters.clear();
        m_lastIndex = -1;
    }

    /**
     * @see java.util.Collection#isEmpty()
     */
    public boolean isEmpty() {
        return m_parameters.isEmpty();
    }

    /**
     * @see Parameters#add(Parameter)
     */
    public boolean add(Parameter parameter) throws NullPointerException {
        if (parameter == null) {
            throw new NullPointerException("parameter=null"); // NPE to follow Collection contract
        }

        m_lastIndex++;
        m_parameters.put(new ParameterNameIndex(parameter.getDefinition().getName(), m_lastIndex), parameter);

        return true;
    }

    /**
     * This is here to satisfy the {@link Collection} interface contract. Same as if calling {@link #contains(String)}
     * where the argument is the parameter name or {@link #contains(Parameter)}.
     *
     * @see java.util.Collection#contains(Object)
     */
    public boolean contains(Object o) throws ClassCastException, NullPointerException {
        if (o == null) {
            throw new NullPointerException("o=null");
        }

        if (o instanceof String) {
            return contains((String) o);
        } else if (o instanceof Parameter) {
            return contains((Parameter) o);
        }

        throw new ClassCastException(LOG.getMsgString(CommI18NResourceKeys.MUST_BE_STRING_OR_PARAM));
    }

    /**
     * @see Parameters#contains(String)
     */
    public boolean contains(String parameterName) throws NullPointerException {
        if (parameterName == null) {
            throw new NullPointerException("parameterName=null");
        }

        return m_parameters.containsKey(new ParameterNameIndex(parameterName, -1));
    }

    /**
     * @see Parameters#contains(Parameter)
     */
    public boolean contains(Parameter parameter) throws NullPointerException {
        if (parameter == null) {
            throw new NullPointerException("parameter=null");
        }

        return contains(parameter.getDefinition().getName());
    }

    /**
     * This is here to satisfy the {@link Collection} interface contract. Same as if calling {@link #remove(String)}
     * where the argument is the parameter name or {@link #remove(Parameter)}.
     *
     * @see java.util.Collection#remove(Object)
     */
    public boolean remove(Object o) throws ClassCastException, NullPointerException {
        if (o == null) {
            throw new NullPointerException("object=null");
        }

        if (o instanceof String) {
            return remove((String) o);
        } else if (o instanceof Parameter) {
            return remove((Parameter) o);
        }

        throw new ClassCastException(LOG.getMsgString(CommI18NResourceKeys.MUST_BE_STRING_OR_PARAM));
    }

    /**
     * Removes the parameter stored in this collection whose name matches <code>parameterName</code>.
     *
     * @param  parameterName the name of the parameter to remove from this collection
     *
     * @return <code>true</code> if the object was removed; <code>false</code> if there was no object in this collection
     *         with the given parameter name
     *
     * @throws NullPointerException if <code>parameterName</code> is <code>null</code>, as per Collection interface
     *                              contract
     */
    public boolean remove(String parameterName) throws NullPointerException {
        if (parameterName == null) {
            throw new NullPointerException("parameterName=null");
        }

        // note that we don't need to mess with the lastIndex - the sorting won't get screwed up
        return (m_parameters.remove(new ParameterNameIndex(parameterName, -1)) != null);
    }

    /**
     * Removes the parameter whose name is the same as the name in the given <code>parameter</code>. The full definition
     * and value of the given <code>parameter</code> is ignored when determining a match - only the parameter name is
     * used in the comparision.
     *
     * @param  parameter the parameter whose name is used to determine what to remove
     *
     * @return <code>true</code> if the object was removed
     *
     * @throws NullPointerException if <code>parameter</code> is <code>null</code>, as per Collection interface contract
     */
    public boolean remove(Parameter parameter) throws NullPointerException {
        if (parameter == null) {
            throw new NullPointerException("parameter=null");
        }

        return remove(parameter.getDefinition().getName());
    }

    /**
     * Adds the elements found in the <code>c</code> collection. If one or more elements in <code>c</code> is not a
     * {@link Parameter}, an exception will be thrown.
     *
     * @see java.util.Collection#addAll(java.util.Collection)
     */
    public boolean addAll(Collection<? extends Parameter> c) throws ClassCastException, NullPointerException {
        if (c == null) {
            throw new NullPointerException("c=null");
        }

        boolean changed = false;

        if (!c.isEmpty()) {
            for (Parameter parameter : c) {
                changed |= add(parameter);
            }
        }

        return changed;
    }

    /**
     * Checks to see if all the elements found in the <code>c</code> collection are in this collection. If one or more
     * elements in <code>c</code> is not a {@link Parameter} or <code>String</code>, an exception will be thrown.
     *
     * @see java.util.Collection#containsAll(Collection)
     */
    public boolean containsAll(Collection<?> c) throws ClassCastException, NullPointerException {
        if (c == null) {
            throw new NullPointerException("c=null");
        }

        boolean hasAll = true;

        if (!c.isEmpty()) {
            for (Object parameter : c) {
                hasAll &= contains(parameter);
            }
        }

        return hasAll;
    }

    /**
     * Removes the elements found in the <code>c</code> collection. If one or more elements in <code>c</code> is not a
     * {@link Parameter}, an exception will be thrown.
     *
     * @see java.util.Collection#removeAll(Collection)
     */
    public boolean removeAll(Collection<?> c) throws ClassCastException, NullPointerException {
        if (c == null) {
            throw new NullPointerException("c=null");
        }

        boolean changed = false;

        if (!c.isEmpty()) {
            for (Object parameter : c) {
                changed |= remove(parameter);
            }
        }

        return changed;
    }

    /**
     * Retains only those elements found in the <code>c</code> collection. If one or more elements in <code>c</code> is
     * not a {@link Parameter}, an exception will be thrown. The implementation will remove all elements in this
     * collection and then add all the elements in <code>c</code>. Because of this, <code>true</code> is always returned
     * - even if the elements end up being the same in the end.
     *
     * @see java.util.Collection#retainAll(Collection)
     */
    public boolean retainAll(Collection<?> c) throws ClassCastException, NullPointerException {
        clear();

        for (Object object : c) {
            add((Parameter) object);
        }

        return true;
    }

    /**
     * The iterator will contain objects of type {@link Parameter}.
     *
     * @see java.util.Collection#iterator()
     */
    public Iterator<Parameter> iterator() {
        return getSortedIterator();
    }

    /**
     * The returned array may be cast to a {@link Parameter} array.
     *
     * @see java.util.Collection#toArray()
     */
    public Object[] toArray() {
        Parameter[] array = new Parameter[m_parameters.size()];

        System.arraycopy(getSortedCollection().toArray(), 0, array, 0, array.length);

        return array;
    }

    /**
     * Returns an array of {@link Parameter} objects.
     *
     * @see java.util.Collection#toArray(T[])
     */
    public <T> T[] toArray(T[] a) {
        return getSortedCollection().toArray(a);
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return m_parameters.toString();
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if ((obj == null) || (!(obj instanceof ParametersImpl))) {
            return false;
        }

        return this.m_parameters.equals(((ParametersImpl) obj).m_parameters);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return m_parameters.hashCode();
    }

    /**
     * This returns a collection that will be sorted - it will have this object's collection elements in order of their
     * index numbers. That is, in the order they were added to the collection.
     *
     * @return Collection that has its elements sorted
     */
    private Collection<Parameter> getSortedCollection() {
        TreeMap<ParameterNameIndex, Parameter> map = new TreeMap<ParameterNameIndex, Parameter>(
            new SortedParameterNameIndexComparator());

        map.putAll(m_parameters);

        return map.values();
    }

    /**
     * This returns an iterator that will return the collection elements in order of their index numbers. That is, in
     * the order they were added to the collection.
     *
     * @return Iterator that returns the elements in order of when they were added
     */
    private Iterator<Parameter> getSortedIterator() {
        return getSortedCollection().iterator();
    }

    /**
     * A proxy to the parent parameters implementation that will only expose a subset of the full parameters collection.
     * This is used to provide a view into the parameters collection that only exposes hidden parameters or only public
     * parameters.
     *
     * <p>A proxy has an associated view mode - hidden only or public only.</p>
     *
     * @see Parameters#getInternalParameters()
     * @see Parameters#getPublicParameters()
     */
    class Proxy implements Parameters {
        /**
         * the UID to identify the serializable version of this class. Be careful if you want to change this -
         * parameters in a serialized form may be persisted.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Defines this proxy's view mode - that is, shows a view of only hidden parameters or public parameters. If
         * <code>true</code>, the proxy only exposes hidden parameters; <code>false</code> for public parameters
         */
        private final boolean m_onlyHidden;

        /**
         * Constructor for {@link Proxy} that defines that proxy's view mode. The view mode defines which parameters
         * this proxy should expose.
         *
         * @param onlyHidden if <code>true</code>, exposes only hidden parameters; if <code>false</code>, exposes only
         *                   public, non-hidden parameters
         */
        Proxy(boolean onlyHidden) {
            m_onlyHidden = onlyHidden;
        }

        /**
         * Only returns the parameter if it is of the proper view type, <code>null</code> otherwise.
         *
         * @see Parameters#getParameter(String)
         */
        public Parameter getParameter(String parameterName) {
            Parameter retParam = ParametersImpl.this.getParameter(parameterName);

            if ((retParam != null) && (retParam.getDefinition() != null)
                && (retParam.getDefinition().isHidden() != m_onlyHidden)) {
                return null;
            }

            return retParam;
        }

        /**
         * Only returns the parameter definition if the parameter is of the proper view type, an exception is thrown
         * otherwise.
         *
         * @see Parameters#getParameterDefinition(String)
         */
        public ParameterDefinition getParameterDefinition(String parameterName)
            throws InvalidParameterDefinitionException {
            Parameter param = getParameter(parameterName);

            if (param == null) {
                throw new InvalidParameterDefinitionException(LOG.getMsgString(CommI18NResourceKeys.NO_PARAMETER,
                    parameterName));
            }

            return param.getDefinition();
        }

        /**
         * Only returns the parameter value if the parameter is of the proper view type, an exception is thrown
         * otherwise.
         *
         * @see Parameters#getParameterValue(String)
         */
        public Object getParameterValue(String parameterName) throws InvalidParameterDefinitionException {
            Parameter param = getParameter(parameterName);

            if (param == null) {
                throw new InvalidParameterDefinitionException(LOG.getMsgString(CommI18NResourceKeys.NO_PARAMETER,
                    parameterName));
            }

            return param.getValue();
        }

        /**
         * Only sets the parameter value if the parameter is of the proper view type, an exception is thrown otherwise.
         *
         * @see Parameters#setParameterValue(String, Object)
         */
        public void setParameterValue(String parameterName, Object parameterValue)
            throws InvalidParameterDefinitionException {
            Parameter param = getParameter(parameterName);

            if (param == null) {
                throw new InvalidParameterDefinitionException(LOG.getMsgString(CommI18NResourceKeys.NO_PARAMETER,
                    parameterName));
            }

            param.setValue(parameterValue);

            return;
        }

        /**
         * If this collection is already exposing the public parameters, a reference to itself is returned. Otherwise,
         * an empty parameters collection is returned to indicate there are no internal parameters in this collection.
         *
         * @see Parameters#getPublicParameters()
         */
        public Parameters getPublicParameters() {
            return (m_onlyHidden) ? ((Parameters) new ParametersImpl()) : ((Parameters) this);
        }

        /**
         * If this collection is already exposing the internal, hidden parameters, a reference to itself is returned.
         * Otherwise, an empty parameters collection is returned to indicate there are no internal parameters in this
         * collection.
         *
         * @see Parameters#getInternalParameters()
         */
        public Parameters getInternalParameters() {
            return (!m_onlyHidden) ? ((Parameters) new ParametersImpl()) : ((Parameters) this);
        }

        /**
         * @see Parameters#applyResourceBundleToParameterRenderingInformation(ResourceBundle)
         */
        public void applyResourceBundleToParameterRenderingInformation(ResourceBundle resourceBundle) {
            for (Iterator iter = this.iterator(); iter.hasNext();) {
                Parameter param = (Parameter) iter.next();
                ParameterDefinition definition = param.getDefinition();
                ParameterRenderingInformation renderingInfo = definition.getRenderingInfo();

                renderingInfo.applyResourceBundle(resourceBundle);
            }
        }

        /**
         * @see Parameters#contains(String)
         */
        public boolean contains(String parameterName) throws NullPointerException {
            return (getParameter(parameterName) != null);
        }

        /**
         * @see Parameters#contains(Parameter)
         */
        public boolean contains(Parameter parameter) throws NullPointerException {
            return (getParameter(parameter.getDefinition().getName()) != null);
        }

        /**
         * As allowed by the Collections API contract, this will throw an <code>IllegalArgumentException</code> if the
         * given parameter is not of the proper view type (that is, if this proxy's view mode is only exposing hidden
         * parameters, you can only add hidden parameters to it; same with public, non-hidden parameters).
         *
         * @see Parameters#add(Parameter)
         */
        public boolean add(Parameter parameter) throws NullPointerException, IllegalArgumentException {
            if (parameter.getDefinition().isHidden() != m_onlyHidden) {
                throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.PARAMETERS_IMPL_HIDDEN,
                    parameter.getName(), parameter.getDefinition().isHidden()));
            }

            return ParametersImpl.this.add(parameter);
        }

        /**
         * @see Parameters#remove(String)
         */
        public boolean remove(String parameterName) throws NullPointerException {
            // make sure you do this contains check - don't want the caller to try to remove something from the parent parameters
            // without ensuring the doomed parameter is of the proper view type (hidden only or public only).
            if (contains(parameterName)) {
                return ParametersImpl.this.remove(parameterName);
            }

            return false;
        }

        /**
         * @see Parameters#remove(Parameter)
         */
        public boolean remove(Parameter parameter) throws NullPointerException {
            // make sure you do this contains check - don't want the caller to try to remove something from the parent parameters
            // without ensuring the doomed parameter is of the proper view type (hidden only or public only).
            if (contains(parameter)) {
                return ParametersImpl.this.remove(parameter);
            }

            return false;
        }

        /**
         * @see java.util.Collection#size()
         */
        public int size() {
            Parameter[] allParams = (Parameter[]) ParametersImpl.this.toArray();
            int retSize = 0;

            for (int i = 0; i < allParams.length; i++) {
                if (allParams[i].getDefinition().isHidden() == m_onlyHidden) {
                    retSize++;
                }
            }

            return retSize;
        }

        /**
         * This clears this proxy's collection by removing all the parameters of the proper view type from the parent
         * parameters collection. For example, if this proxy is exposing only hidden parameters, all hidden parameters
         * will be removed from the parent parameters collection leaving all public parameters intact.
         *
         * @see java.util.Collection#clear()
         */
        public void clear() {
            Parameter[] allParams = (Parameter[]) ParametersImpl.this.toArray();

            for (int i = 0; i < allParams.length; i++) {
                if (allParams[i].getDefinition().isHidden() == m_onlyHidden) {
                    ParametersImpl.this.remove(allParams[i]);
                }
            }

            return;
        }

        /**
         * @see java.util.Collection#isEmpty()
         */
        public boolean isEmpty() {
            return (size() == 0);
        }

        /**
         * @see java.util.Collection#toArray()
         */
        public Object[] toArray() {
            Parameter[] allArray = (Parameter[]) ParametersImpl.this.toArray();
            Parameter[] retArray = new Parameter[size()];

            if (retArray.length > 0) {
                // doing it this way ensures the ordering of the parameters stays the same as in the parent parameters collection
                int retIndex = 0;
                for (int i = 0; i < allArray.length; i++) {
                    if (allArray[i].getDefinition().isHidden() == m_onlyHidden) {
                        retArray[retIndex++] = allArray[i]; // if array-out-of-bounds occurs, someone concurrently modified the parent map
                    }
                }
            }

            return retArray;
        }

        /**
         * @see java.util.Collection#contains(Object)
         */
        public boolean contains(Object o) throws ClassCastException, NullPointerException {
            if (o == null) {
                throw new NullPointerException("o=null");
            }

            if (o instanceof String) {
                return contains((String) o);
            } else if (o instanceof Parameter) {
                return contains((Parameter) o);
            }

            throw new ClassCastException(LOG.getMsgString(CommI18NResourceKeys.MUST_BE_STRING_OR_PARAM));
        }

        /**
         * @see java.util.Collection#remove(Object)
         */
        public boolean remove(Object o) throws ClassCastException, NullPointerException {
            if (o == null) {
                throw new NullPointerException("o=null");
            }

            if (o instanceof String) {
                return remove((String) o);
            } else if (o instanceof Parameter) {
                return remove((Parameter) o);
            }

            throw new ClassCastException(LOG.getMsgString(CommI18NResourceKeys.MUST_BE_STRING_OR_PARAM));
        }

        /**
         * As allowed by the Collections API contract, this will throw an <code>IllegalArgumentException</code> if the
         * given parameter's object is a parameter that is not of the proper view type (that is, if this proxy's view
         * mode is only exposing hidden parameters, you can only add hidden parameters to it; same with public,
         * non-hidden parameters).
         *
         * @see java.util.Collection#addAll(Collection)
         */
        public boolean addAll(Collection<? extends Parameter> c) throws ClassCastException, NullPointerException,
            IllegalArgumentException {
            if (c == null) {
                throw new NullPointerException("c=null");
            }

            boolean changed = false;

            if (!c.isEmpty()) {
                for (Parameter parameter : c) {
                    changed |= add(parameter);
                }
            }

            return changed;
        }

        /**
         * @see java.util.Collection#containsAll(Collection)
         */
        public boolean containsAll(Collection c) throws ClassCastException, NullPointerException {
            if (c == null) {
                throw new NullPointerException("c=null");
            }

            boolean hasAll = true;

            if (!c.isEmpty()) {
                for (Iterator iter = c.iterator(); iter.hasNext() && hasAll;) {
                    Object element = iter.next();
                    hasAll &= contains(element);
                }
            }

            return hasAll;
        }

        /**
         * @see java.util.Collection#removeAll(Collection)
         */
        public boolean removeAll(Collection c) {
            if (c == null) {
                throw new NullPointerException("c=null");
            }

            boolean changed = false;

            if (!c.isEmpty()) {
                for (Iterator iter = c.iterator(); iter.hasNext();) {
                    Object element = iter.next();
                    changed |= remove(element);
                }
            }

            return changed;
        }

        /**
         * @see java.util.Collection#retainAll(Collection)
         */
        public boolean retainAll(Collection<?> c) {
            clear();

            for (Object object : c) {
                add((Parameter) object);
            }

            return true;
        }

        /**
         * @see java.util.Collection#iterator()
         */
        public Iterator<Parameter> iterator() {
            TreeMap<ParameterNameIndex, Parameter> map = new TreeMap<ParameterNameIndex, Parameter>(
                new SortedParameterNameIndexComparator());

            // only put those parameters with the proper view type will be put in our tree map
            Set<Map.Entry<ParameterNameIndex, Parameter>> entrySet = m_parameters.entrySet();
            for (Map.Entry<ParameterNameIndex, Parameter> entry : entrySet) {
                Parameter param = entry.getValue();
                if (param.getDefinition().isHidden() == m_onlyHidden) {
                    map.put(entry.getKey(), param);
                }
            }

            return map.values().iterator();
        }

        /**
         * @see java.util.Collection#toArray(T[])
         */
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            Object[] retArray;
            Object[] proxyParameters = toArray(); // gets just those parameters exposed by the proxy

            if (a.length >= proxyParameters.length) {
                System.arraycopy(proxyParameters, 0, a, 0, proxyParameters.length);
                if (a.length > proxyParameters.length) {
                    a[proxyParameters.length] = null; // as per Collections toArray(Object[]) contract - see its javadoc
                }

                retArray = a;
            } else {
                retArray = proxyParameters;
            }

            return (T[]) retArray; // this cast is OK, we know all its items conform to the type T (which is Parameter)
        }
    }
}