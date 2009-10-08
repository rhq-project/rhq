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
package org.rhq.enterprise.communications.command;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import org.rhq.enterprise.communications.command.param.InvalidParameterValueException;
import org.rhq.enterprise.communications.command.param.NoParameterDefinitionsException;
import org.rhq.enterprise.communications.command.param.ParameterDefinition;

/**
 * Defines a command that can be executed. A command consists of a {@link #getCommandType() type} (e.g. the command
 * name) and an optional set of {@link ParameterDefinition parameters}, whose values can be obtained via
 * {@link #getParameterValue(String)}.
 *
 * <p>If a command accepts any and all parameter types and values, it does not need to define a set of parameter
 * definitions. However, if a command accepts only a certain set of parameters, it must define a set of parameter
 * definitions that determine the validity of a command's parameter values.</p>
 *
 * <p>Note that command implementors are recommended to provide a set of parameter setter methods should they require
 * parameters in order to be able to execute. This will allow a strongly-typed mechanism to be available in order to set
 * parameters (as opposed to asking the client to add Object values to a weakly-typed map and/or checking the parameter
 * definitions to ensure the parameter values are valid). It is highly recommended that the implementors of this
 * interface define setter methods that take Objects as opposed to primitives (e.g. <code>Integer</code> vs. <code>
 * int</code>) to make it easier for cmdline clients to {@link ParameterDefinition#convertObject(Object) convert} from
 * text-based input to the Java representations of the actual data types needed.</p>
 *
 * @author John Mazzitelli
 */
public interface Command extends Serializable {
    /**
     * Returns an object that identifies the type of command.
     *
     * @return command type identifier (will never be <code>null</code>)
     */
    CommandType getCommandType();

    /**
     * Returns <code>true</code> if this command does not utilize
     * {@link #getParameterDefinitions() parameter definitions} to restrict what parameters it can accept. <code>
     * true</code> means this command will accept any and all parameter values and types. <code>false</code> means that
     * this command restricts the kinds of parameters it will accept. The definitions of the allowable parameters are
     * determined by a call to {@link #getParameterDefinitions()}.
     *
     * @return <code>true</code> if there are no restrictions to the number, types and names of parameters accepted by
     *         the command; <code>false</code> means all parameters must pass validity checks based on parameter
     *         definitions found in {@link #getParameterDefinitions()}
     */
    boolean allowAnyParameter();

    /**
     * Returns the set of parameter definitions for all allowable parameters this command accepts. If a command does not
     * accept any parameters, an empty array must be returned. If a command has not defined any definitions (i.e. will
     * accept any and all parameters), an exception is thrown.
     *
     * <p>Whether a command accepts any and all parameters can also be determined by calling
     * {@link #allowAnyParameter()}.</p>
     *
     * <p>Command implementations may choose to make the order of the returned array significant.</p>
     *
     * @return array of parameter definitions (may be <code>null</code>)
     *
     * @throws NoParameterDefinitionsException if no parameter definitions have been defined by the command; this
     *                                         indicates the command will accept any and all parameters
     */
    ParameterDefinition[] getParameterDefinitions() throws NoParameterDefinitionsException;

    /**
     * Returns the definition of the named parameter. You can use this method to determine if a command accepts a
     * parameter with a given name. If a command does not accept a parameter with the specified name, <code>null</code>
     * is returned. If a command does not define parameter definitions (e.g. {@link #allowAnyParameter()} returns <code>
     * true</code>}), an exception is thrown.
     *
     * @param  paramName the name of the parameter whose definition is to be returned (must not be <code>null</code>)
     *
     * @return definition of the named parameter or <code>null</code> if the command does not accept a parameter with
     *         the given name
     *
     * @throws IllegalArgumentException        if <code>paramName</code> is <code>null</code>
     * @throws NoParameterDefinitionsException if the command has not defined any parameter definitions
     */
    ParameterDefinition getParameterDefinition(String paramName) throws IllegalArgumentException,
        NoParameterDefinitionsException;

    /**
     * Checks to see if the given parameter has explicitly been given a value (<code>null</code> or otherwise).
     *
     * @param  paramName the name of the parameter whose existence is to be checked
     *
     * @return <code>true</code> if a parameter named <code>paramName</code> has a value explicitly defined in this
     *         command; <code>false</code> if there is no parameter of that name defined
     *
     * @throws IllegalArgumentException if <code>paramName</code> is <code>null</code>
     */
    boolean hasParameterValue(String paramName) throws IllegalArgumentException;

    /**
     * Returns the values of all parameters that are to be passed to the command in a <code>Map</code>, with the keys in
     * the map being the parameter names.
     *
     * <p>Note that the returned <code>Map</code> is a copy - changes to the <code>Map</code> will not reflect back into
     * the command. However, each individual value object is the original reference; it is recommended that the caller
     * not modify the underlying value objects found in the returned map; instead,
     * {@link #setParameterValue(String, Object)} should be used.</p>
     *
     * @return the parameter name/value pairs (will not be <code>null</code> but may be empty)
     */
    Map<String, Object> getParameterValues();

    /**
     * Returns the value of a parameter that is to be passed to the command.
     *
     * @param  paramName the name of the parameter whose value is to be returned (must not be <code>null</code>)
     *
     * @return the parameter value (may be <code>null</code>)
     *
     * @throws IllegalArgumentException if <code>paramName</code> is <code>null</code>
     */
    Object getParameterValue(String paramName) throws IllegalArgumentException;

    /**
     * Adds a parameter value under the given parameter name. If a parameter already exists under the given name, it's
     * value is overwritten with the given <code>paramValue</code>. Note that any parameter name or value may be added
     * with this method - validity checking is not performed. To ensure a command's parameters are valid, call
     * {@link #checkParameterValidity(boolean)}.
     *
     * <p>Note that implementations should not rely on clients using this method - instead, implementations of this
     * interface should provide additional, more strongly typed, setter methods. This provides two things: first, it
     * does not ask the client to know the actual parameter name and second it provides stronger type checking (as
     * opposed to being able to just pass in an <code>Object</code> as this method allows).</p>
     *
     * @param  paramName  the name of the new parameter
     * @param  paramValue the value of the new parameter
     *
     * @throws IllegalArgumentException if <code>paramName</code> is <code>null</code>
     */
    void setParameterValue(String paramName, Object paramValue) throws IllegalArgumentException;

    /**
     * Removes a parameter from this command. Validity checks are not made - to validate the parameter values after this
     * call is made, see {@link #checkParameterValidity(boolean)}.
     *
     * <p>Also see {@link #setParameterValue(String, Object)} for the recommendation of defining strongly-typed methods
     * in implementation classes.</p>
     *
     * @param  paramName a key to identify the parameter to remove (may be <code>null</code>)
     *
     * @throws IllegalArgumentException if <code>paramName</code> is <code>null</code>
     */
    void removeParameterValue(String paramName) throws IllegalArgumentException;

    /**
     * Removes all parameters from this command. Validity checks are not made - to validate the empty command after this
     * call is made, see {@link #checkParameterValidity(boolean)}.
     */
    void removeParameterValues();

    /**
     * If <code>true</code> is returned, then this {@link Command} that was executed will be included in the returned
     * response. This is useful if executed asynchronously and the command requestor will need to know the command and
     * the parameters that were issued. If <code>false</code>, then the response returned by the server after the
     * command has executed will be unavailable (i.e. <code>null</code>).
     *
     * @return <code>true</code> if this object will be returned in the response to the command execution
     */
    boolean isCommandInResponse();

    /**
     * Sets a flag to determine if this {@link Command} object is to be returned with the response after the command has
     * executed. Using this will affect performance as it requires not only for the actual response data to be
     * (un)marshalled, but also the {@link Command} must be (un)marshalled and set over the network as well. Use this if
     * the command was executed asynchronously and the original {@link Command} object is no longer available on the
     * client.
     *
     * @param flag <code>true</code> to get this object returned with the command response.
     */
    void setCommandInResponse(boolean flag);

    /**
     * This method verifies the validity of the parameters. If one or more parameters are invalid for some reason (e.g.
     * a required parameter is missing or parameters exist that are not used by the command) this method will throw an
     * exception, with the exception message describing the erroneous condition (i.e. identifying the invalid
     * parameter(s)). When this occurs, the command should not be invoked.
     *
     * <p>If <code>convertIfNecessary</code> is <code>true</code>, this method will attempt to convert a parameter whose
     * value did not match that of its parameter definition. This conversion is not guaranteed to be successful; if the
     * conversion fails, the original parameter value remains intact and the validity check fails. If the conversion
     * succeeds, the newly converted parameter value replaces the invalid value.</p>
     *
     * <p>If all parameters are deemed valid and the command can be invoked, this method does nothing.</p>
     *
     * @param  convertIfNecessary if <code>true</code>, then attempt to convert any invalid parameter values
     *
     * @throws InvalidParameterValueException if one or more parameters are invalid
     *
     * @see    #convertParameters()
     */
    void checkParameterValidity(boolean convertIfNecessary) throws InvalidParameterValueException;

    /**
     * This method will attempt to convert all parameters whose types did not match that of its corresponding parameter
     * definition. This conversion is not guaranteed to be successful; if the conversion fails, the original parameter
     * value remains intact. If the conversion succeeds, the newly converted parameter value replaces the invalid value.
     *
     * @see #checkParameterValidity(boolean)
     */
    void convertParameters();

    /**
     * Returns a map of name/value pairs that can be used to configure this particular instance of the command. This is
     * useful when you need to relate the execution of a particular command instance to something else (like an
     * asynchronous response to the command) or if you need some out-of-band metadata that is required to configure the
     * command. Nothing necessarily has to be defined in this map, but all commands must have a configuration properties
     * map available.
     *
     * @return this instance's configuration map of name/value properties
     */
    Properties getConfiguration();
}