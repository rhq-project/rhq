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

import java.io.Serializable;
import java.util.Collection;
import java.util.ResourceBundle;

/**
 * Defines a collection of parameters.
 *
 * @author John Mazzitelli
 */
public interface Parameters extends Collection<Parameter>, Serializable {
    /**
     * Given the name of a parameter, this will return its associated {@link Parameter}. If it doesn't exist in this
     * collection, <code>null</code> is returned.
     *
     * @param  parameterName the name of the parameter to get
     *
     * @return the parameter definition and value of the named parameter
     */
    Parameter getParameter(String parameterName);

    /**
     * Given the name of a parameter, this will return the parameter's definition. It is possible that a parameter
     * exists, but its definition is <code>null</code>; in this case, <code>null</code> is returned. If the parameter
     * does not exist in this collection, an exception is thrown.
     *
     * @param  parameterName the name of the parameter whose definition is to be returned
     *
     * @return the parameter definition of the named parameter
     *
     * @throws InvalidParameterDefinitionException if the parameter does not exist in this collection
     */
    ParameterDefinition getParameterDefinition(String parameterName) throws InvalidParameterDefinitionException;

    /**
     * Given the name of a parameter, this will return the parameter's value. Since a parameter's value is allowed to be
     * <code>null</code>, a return value of <code>null</code> means the parameter exists but its value was <code>
     * null</code>. If the parameter does not exist in this collection, an exception is thrown.
     *
     * @param  parameterName the name of the parameter whose value is to be returned
     *
     * @return the parameter value of the named parameter
     *
     * @throws InvalidParameterDefinitionException if the parameter does not exist in this collection
     */
    Object getParameterValue(String parameterName) throws InvalidParameterDefinitionException;

    /**
     * Given the name of a parameter, this will set the parameter's value. If the parameter doesn't exist in this
     * collection, an exception is thrown.
     *
     * @param  parameterName  the name of the parameter whose value is to be set
     * @param  parameterValue the value of the parameter (may be <code>null</code>)
     *
     * @throws InvalidParameterDefinitionException if the parameter does not exist in this collection
     */
    void setParameterValue(String parameterName, Object parameterValue) throws InvalidParameterDefinitionException;

    /**
     * Returns a {@link ParametersImpl} collection that consists of this object's
     * {@link ParameterDefinition#isHidden() non-hidden} parameters. In effect, this returns parameters that are
     * typically for clients (such as a GUI) to provide values for.
     *
     * @return collection of public, non-hidden parameters
     */
    Parameters getPublicParameters();

    /**
     * Returns a {@link ParametersImpl} collection that consists of this object's
     * {@link ParameterDefinition#isHidden() hidden} parameters. In effect, this returns only those parameters that are
     * used internally - clients should not care about them or set their values. These are typically internal parameters
     * for use by the executor but whose values are normally set without asking the client for the values.
     *
     * @return collection of internal (i.e. hidden) parameters
     */
    Parameters getInternalParameters();

    /**
     * This will apply the given <code>resourceBundle</code> to the parameters in this collection. This allows you to
     * localize the parameters.
     *
     * @param resourceBundle the resource bundle that is to be used when rendering the parameters
     */
    void applyResourceBundleToParameterRenderingInformation(ResourceBundle resourceBundle);

    /**
     * Checks if this parameter exists by specifying the {@link ParameterDefinition#getName() parameter name}.
     *
     * @param  parameterName the parameter name
     *
     * @return <code>true</code> if there is a parameter in this object identified by the <code>parameterName</code>
     *
     * @throws NullPointerException if <code>parameterName</code> is <code>null</code>, as per Collection interface
     *                              contract
     */
    boolean contains(String parameterName) throws NullPointerException;

    /**
     * Checks to see if this parameter exists by specifying the {@link Parameter parameter} - only the name is used when
     * checking. This means as long as the given parameter's name matches one in the collection, this returns <code>
     * true</code>, it doesn't matter if the parameter's value or definition completely match.
     *
     * @param  parameter the parameter to check
     *
     * @return <code>true</code> if there is a parameter in this object identified by the <code>parameterName</code>
     *
     * @throws NullPointerException if <code>parameter</code> is <code>null</code>, as per Collection interface contract
     */
    boolean contains(Parameter parameter) throws NullPointerException;

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
    boolean remove(String parameterName) throws NullPointerException;

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
    boolean remove(Parameter parameter) throws NullPointerException;
}