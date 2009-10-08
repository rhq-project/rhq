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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.command.param.InvalidParameterDefinitionException;
import org.rhq.enterprise.communications.command.param.InvalidParameterValueException;
import org.rhq.enterprise.communications.command.param.NoParameterDefinitionsException;
import org.rhq.enterprise.communications.command.param.OptionalParameterDefinitionIterator;
import org.rhq.enterprise.communications.command.param.ParameterDefinition;
import org.rhq.enterprise.communications.command.param.RequiredParameterDefinitionIterator;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Superclass to all {@link Command} objects that may be executed by a command processor.
 *
 * @author John Mazzitelli
 */
public abstract class AbstractCommand implements Command {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(AbstractCommand.class);

    /**
     * this command's type, which includes the name of the command
     */
    private CommandType m_commandType;

    /**
     * this command's parameter definitions; may be <code>null</code> if all parameters are accepted
     */
    private Map<String, ParameterDefinition> m_parameterDefinitions;

    /**
     * optional parameters to this command; may be <code>null</code>
     */
    private Map<String, Object> m_commandParameters;

    /**
     * if <code>true</code>, the command processor will return this command in the response back to the client
     */
    private boolean m_commandInResponse;

    /**
     * This instance's configuration.
     */
    private Properties m_config;

    /**
     * the Serializable UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@link AbstractCommand} that defines just the type without any parameters.
     *
     * @throws IllegalArgumentException            if <code>commandType</code> was not defined by the command subclass
     * @throws InvalidParameterDefinitionException if more than one parameter is defined with the same name
     *
     * @see    #buildCommandType()
     * @see    #buildParameterDefinitions()
     * @see    #AbstractCommand(Map)
     */
    public AbstractCommand() throws IllegalArgumentException, InvalidParameterDefinitionException {
        this((Map<String, Object>) null); // by default, there are no extra parameters
    }

    /**
     * Constructor for {@link AbstractCommand} that allows the caller to define both the name and parameter default
     * values.
     *
     * @param  commandParameters optional set of parameters to be passed with the command (may be empty or <code>
     *                           null</code>)
     *
     * @throws IllegalArgumentException            if <code>commandType</code> was not defined by the command subclass
     * @throws InvalidParameterDefinitionException if more than one parameter is defined with the same name
     *
     * @see    #buildCommandType()
     * @see    #buildParameterDefinitions()
     */
    @SuppressWarnings("unchecked")
    public AbstractCommand(Map<String, Object> commandParameters) throws IllegalArgumentException,
        InvalidParameterDefinitionException {
        initializeMetadata();

        m_commandParameters = commandParameters;
        m_commandInResponse = false; // by default, don't return this object back with the response
        m_config = new Properties();

        return;
    }

    /**
     * Constructor for {@link AbstractCommand} that acts as a pseudo-copy constructor and a command
     * decorator/transformer. The given command's parameter values will be copied to this new command object. However,
     * the command type and the parameter definitions will be those defined by the subclass'
     * {@link #buildParameterDefinitions()}, not those of the given command.
     *
     * <p>So, as you can see, this isn't truely a follower of the Decorator pattern or a copy-constructor; instead,
     * think of this constructor as a transformer from one command implementation to another, while maintaining the
     * command's original parameter values (as well as the {@link Command#isCommandInResponse() flag}).</p>
     *
     * <p>This is typically used when the given command is a generic command (one with no parameter definitions for
     * example) and the caller wants to convert it to a more concrete command implementation. This is usually due to the
     * fact that the creator of the given command object did not know at compile time the specific concrete command type
     * it needed.</p>
     *
     * <p>Transforming a command allows the caller to "decorate" the given command with a concrete command
     * implementation's API (which typically has more strongly typed methods to extract out command parameters).</p>
     *
     * <p>If the given <code>commandToTransform</code>
     * {@link Command#allowAnyParameter() allowed any and all parameters} but the newly transformed command (this
     * object) does not, this method forces the current set of parameters to be validated (after possibly being
     * converted to the appropriate types). If the parameters do not validate, an exception will be thrown.</p>
     *
     * <p>The only thing a subclass must do in order to support this transformer constructor is to override it and call
     * it via <code>super</code>.</p>
     *
     * @param  commandToTransform the command object to transform into this class type
     *
     * @throws InvalidParameterValueException if the original command's parameters are not valid for this command
     */
    public AbstractCommand(Command commandToTransform) throws InvalidParameterValueException {
        // build our subclass' command type and parameter definitions
        initializeMetadata();

        //copy the rest of the command's data from the given command to this new command object
        m_commandInResponse = commandToTransform.isCommandInResponse();

        Map<String, Object> parameterValues = commandToTransform.getParameterValues();
        if ((parameterValues != null) && (parameterValues.size() > 0)) {
            m_commandParameters = new HashMap<String, Object>();
            m_commandParameters.putAll(parameterValues);
        }

        if (commandToTransform.allowAnyParameter() && !this.allowAnyParameter()) {
            checkParameterValidity(true);
        }

        m_config = new Properties();

        if (commandToTransform.getConfiguration() != null) {
            m_config.putAll(commandToTransform.getConfiguration());
        }

        return;
    }

    /**
     * @see Command#getCommandType()
     */
    public CommandType getCommandType() {
        return m_commandType;
    }

    /**
     * @see Command#isCommandInResponse()
     */
    public boolean isCommandInResponse() {
        return m_commandInResponse;
    }

    /**
     * @see Command#setCommandInResponse(boolean)
     */
    public void setCommandInResponse(boolean flag) {
        m_commandInResponse = flag;
    }

    /**
     * @see Command#allowAnyParameter()
     */
    public boolean allowAnyParameter() {
        return (m_parameterDefinitions == null);
    }

    /**
     * @see Command#getParameterDefinition(String)
     */
    public ParameterDefinition getParameterDefinition(String paramName) throws IllegalArgumentException,
        NoParameterDefinitionsException {
        if (m_parameterDefinitions == null) {
            throw new NoParameterDefinitionsException(LOG.getMsgString(CommI18NResourceKeys.NO_PARAM_DEF_ACCEPTS_ALL));
        }

        if (paramName == null) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.NULL_PARAM_NAME));
        }

        return m_parameterDefinitions.get(paramName);
    }

    /**
     * @see Command#getParameterDefinitions()
     */
    public ParameterDefinition[] getParameterDefinitions() throws NoParameterDefinitionsException {
        if (m_parameterDefinitions == null) {
            throw new NoParameterDefinitionsException(LOG.getMsgString(CommI18NResourceKeys.NO_PARAM_DEF_ACCEPTS_ALL));
        }

        Collection<ParameterDefinition> paramDefs = m_parameterDefinitions.values();

        return paramDefs.toArray(new ParameterDefinition[paramDefs.size()]);
    }

    /**
     * @see Command#hasParameterValue(String)
     */
    public boolean hasParameterValue(String paramName) throws IllegalArgumentException {
        if (paramName == null) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.NULL_PARAM_NAME));
        }

        boolean hasValue = false;

        if (m_commandParameters != null) {
            hasValue = m_commandParameters.containsKey(paramName);
        }

        return hasValue;
    }

    /**
     * @see Command#getParameterValues()
     */
    public Map<String, Object> getParameterValues() throws IllegalArgumentException {
        Map<String, Object> retValues = new HashMap<String, Object>();

        if (m_commandParameters != null) {
            retValues.putAll(m_commandParameters);
        }

        return retValues;
    }

    /**
     * @see Command#getParameterValue(String)
     */
    public Object getParameterValue(String paramName) {
        if (paramName == null) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.NULL_PARAM_NAME));
        }

        Object retValue = null;

        if (m_commandParameters != null) {
            retValue = m_commandParameters.get(paramName);
        }

        return retValue;
    }

    /**
     * @see Command#setParameterValue(String, Object)
     */
    public void setParameterValue(String paramName, Object paramValue) throws IllegalArgumentException {
        if (paramName == null) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.NULL_PARAM_NAME_SET));
        }

        // if there is no parameters map yet, create one now
        if (m_commandParameters == null) {
            m_commandParameters = new HashMap<String, Object>();
        }

        m_commandParameters.put(paramName, paramValue);

        return;
    }

    /**
     * @see Command#removeParameterValue(String)
     */
    public void removeParameterValue(String paramName) {
        if (paramName == null) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.NULL_PARAM_NAME_REMOVE));
        }

        if (m_commandParameters != null) {
            m_commandParameters.remove(paramName);

            if (m_commandParameters.isEmpty()) {
                m_commandParameters = null;
            }
        }

        return;
    }

    /**
     * @see Command#removeParameterValues()
     */
    public void removeParameterValues() {
        if (m_commandParameters != null) {
            m_commandParameters.clear();
            m_commandParameters = null;
        }

        return;
    }

    /**
     * Returns the string representation of this command that includes the parameter values but not the parameter
     * definitions. See {@link #toString(boolean, boolean)} if you want the string to include the definitions or omit
     * the parameter values.
     *
     * @see Object#toString()
     */
    public String toString() {
        return toString(true, false);
    }

    /**
     * Returns a toString representation of the command but provides the caller the option to include or omit the
     * parameters values and/or the parameter definitions.
     *
     * <p>Parameter definitions are noisy and can make the string very verbose.</p>
     *
     * @param  includeParameters    if <code>true</code>, show the parameter values in the returned string
     * @param  includeParameterDefs if <code>true</code>, show the parameter definitions in the returned string
     *
     * @return string describing the command
     *
     * @see    Object#toString()
     */
    public String toString(boolean includeParameters, boolean includeParameterDefs) {
        StringBuffer strBuf = new StringBuffer("Command: type=[");
        strBuf.append(getCommandType());
        strBuf.append("]; cmd-in-response=[");
        strBuf.append(m_commandInResponse);
        strBuf.append("]; config=[");
        strBuf.append(m_config);

        if (includeParameters) {
            strBuf.append("]; params=[");
            strBuf.append(m_commandParameters);
        }

        if (includeParameterDefs) {
            strBuf.append("]; param-defs=[");
            strBuf.append(m_parameterDefinitions);
        }

        strBuf.append("]");

        return strBuf.toString();
    }

    /**
     * @see Command#checkParameterValidity(boolean)
     */
    public void checkParameterValidity(boolean convertIfNecessary) throws InvalidParameterValueException {
        // return immediately if this command accepts any and all parameters
        if (allowAnyParameter()) {
            return;
        }

        // placeholder to remember parameter values as we check them
        Object paramValue;

        // get the names of all the command's current set of parameters
        Set existingParamNames;

        if (m_commandParameters != null) {
            existingParamNames = m_commandParameters.keySet();
        } else {
            existingParamNames = new HashSet();
        }

        // make sure the current set of parameters have all required parameters defined with the proper types
        for (Iterator iter = new RequiredParameterDefinitionIterator(m_parameterDefinitions.values()); iter.hasNext();) {
            ParameterDefinition paramDef = (ParameterDefinition) iter.next();
            if (!existingParamNames.contains(paramDef.getName())) {
                throw new InvalidParameterValueException(LOG.getMsgString(CommI18NResourceKeys.MISSING_REQUIRED_FIELD,
                    paramDef.getName(), this));
            }

            paramValue = getParameterValue(paramDef.getName());
            if (!paramDef.isValidValue(paramValue)) {
                boolean valid = false;

                if (convertIfNecessary) {
                    paramValue = paramDef.convertObject(paramValue);

                    // if we got this far, our invalid value was converted to a valid value
                    valid = true;

                    // overwrite the invalid value with the new valid one
                    m_commandParameters.put(paramDef.getName(), paramValue);
                }

                if (!valid) {
                    throw new InvalidParameterValueException(LOG.getMsgString(
                        CommI18NResourceKeys.BAD_REQUIRED_PARAM_TYPE, paramDef.getName(), paramDef.getType(), paramDef
                            .isNullable(), ((paramValue == null) ? "<null>" : paramValue.getClass().toString()), this));
                }
            }
        }

        // if the current set of parameters has optional parameters defined,
        // make sure their values are of the proper type
        for (Iterator iter = new OptionalParameterDefinitionIterator(m_parameterDefinitions.values()); iter.hasNext();) {
            ParameterDefinition paramDef = (ParameterDefinition) iter.next();
            if (existingParamNames.contains(paramDef.getName())) {
                paramValue = getParameterValue(paramDef.getName());
                if (!paramDef.isValidValue(paramValue)) {
                    boolean valid = false;
                    if (convertIfNecessary) {
                        paramValue = paramDef.convertObject(paramValue);

                        // if we got this far, our invalid value was converted to a valid value
                        valid = true;

                        // overwrite the invalid value with the new valid one
                        m_commandParameters.put(paramDef.getName(), paramValue);
                    }

                    if (!valid) {
                        throw new InvalidParameterValueException(LOG.getMsgString(
                            CommI18NResourceKeys.BAD_OPTIONAL_PARAM_TYPE, paramDef.getName(), paramDef.getType(),
                            paramDef.isNullable(),
                            ((paramValue == null) ? "<null>" : paramValue.getClass().toString()), this));
                    }
                }
            }
        }

        // now make sure the current set of parameters do not contain unused, extra parameters
        for (Iterator iter = existingParamNames.iterator(); iter.hasNext();) {
            String paramName = (String) iter.next();

            if (!m_parameterDefinitions.containsKey(paramName)) {
                throw new InvalidParameterValueException(LOG.getMsgString(CommI18NResourceKeys.UNEXPECTED_PARAM,
                    paramName, this));
            }
        }

        // everything checks out OK
        return;
    }

    /**
     * @see Command#convertParameters()
     */
    public void convertParameters() {
        // if any parameters are allow, or we have no parameters at all, then just return
        if (allowAnyParameter() || (m_commandParameters == null)) {
            return;
        }

        // we put all parameters that we've converted in here, key=param name, value=the new converted value
        Map<String, Object> convertedParameters = new HashMap<String, Object>();

        for (Iterator iter = m_commandParameters.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            String paramName = (String) entry.getKey();
            Object paramValue = entry.getValue();

            try {
                ParameterDefinition def = getParameterDefinition(paramName);

                // if there is no definition defined, any value goes - don't bother trying to convert if no definition is available
                if (def != null) {
                    Object convertedParamValue = def.convertObject(paramValue);

                    // overwrite the old value with the new one of the proper, converted type
                    // the references will be identical if no conversion was performed (i.e. the original value was of the proper type)
                    if (convertedParamValue != paramValue) {
                        convertedParameters.put(paramName, convertedParamValue);
                    }
                }
            } catch (NoParameterDefinitionsException ignore) // we checked that !allowAnyParameter, this shouldn't occur
            {
                throw new IllegalStateException(LOG.getMsgString(CommI18NResourceKeys.SHOULD_NOT_OCCUR));
            }
        }

        m_commandParameters.putAll(convertedParameters);

        return;
    }

    /**
     * @see Command#getConfiguration()
     */
    public Properties getConfiguration() {
        return m_config;
    }

    /**
     * Returns a <code>Map</code> containing all optional parameters set on this command. This may return <code>
     * null</code> in which case no parameters will be sent along with the command when it is to be executed.
     *
     * <p>Note: the returned <code>Map</code> is not a copy - changes made to the returned object are reflected back
     * into this command object.</p>
     *
     * @return map containing all parameters (may be empty or <code>null</code>)
     */
    protected Map getParameterValuesInternalMap() {
        return m_commandParameters;
    }

    /**
     * Sets the parameter map, overwriting any previously existing map of parameters. Note the scope is <code>
     * protected</code> - users of commands should not be allowed to insert a new map of parameters; rather, subclasses
     * should provide setters for individual parameter values.
     *
     * @param commandParameters the new map of parameters (may be <code>null</code>)
     */
    protected void setParameterValuesInternalMap(Map<String, Object> commandParameters) {
        m_commandParameters = commandParameters;
    }

    /**
     * Initializes the newly constructed command so its type and parameter definition metadata are built properly.
     *
     * <p>This method is called from the constructors. However, it is protected to allow for specialized command
     * implementations to be able to re-initialize the command metadata.</p>
     *
     * @throws IllegalArgumentException            if the command type was not specified by the command subclass
     * @throws InvalidParameterDefinitionException if more than one parameter is defined with the same name
     *
     * @see    #buildCommandType()
     * @see    #buildParameterDefinitions()
     * @see    #AbstractCommand(Map)
     */
    protected void initializeMetadata() throws IllegalArgumentException, InvalidParameterDefinitionException {
        // the command subclass will tell us what its type and parameter definitions are
        CommandType commandType = buildCommandType();
        ParameterDefinition[] parameterDefinitions = buildParameterDefinitions();

        if (commandType == null) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.NULL_COMMAND_TYPE));
        }

        m_commandType = commandType;

        if (parameterDefinitions != null) {
            // for easier retrieval, store each definition in a Map keyed on the parameter name
            m_parameterDefinitions = new HashMap<String, ParameterDefinition>();

            for (int i = 0; i < parameterDefinitions.length; i++) {
                ParameterDefinition paramDef = parameterDefinitions[i];
                String paramName = paramDef.getName();

                if (null != m_parameterDefinitions.put(paramName, paramDef)) {
                    // this is bad - it probably means the command is coded wrong and will never be able to be used
                    throw new InvalidParameterDefinitionException(LOG.getMsgString(CommI18NResourceKeys.DUP_DEFS,
                        paramName));
                }
            }
        } else {
            m_parameterDefinitions = null;
        }

        return;
    }

    /**
     * Builds the command type of this command. It must not be <code>null</code>.
     *
     * <p>This method is called by the command's constructors.</p>
     *
     * @return the command's type definition; must not be <code>null</code>
     */
    protected abstract CommandType buildCommandType();

    /**
     * Builds the set of parameter definitions that this command will use. Parameter definitions define what parameters
     * this command accepts. Implementors must return one of the following:
     *
     * <ul>
     *   <li>An array of one or more definitions if the command accepts required and/or optional parameters</li>
     *   <li>An empty array if the command does not accept <i>any</i> parameters (i.e. a no-arg command)</li>
     *   <li><code>null</code> if the command accepts any and all parameters, regardless of name or type</li>
     * </ul>
     *
     * <p>This method is called by the command's constructors.</p>
     *
     * @return an array of parameter definitions or <code>null</code>
     */
    protected abstract ParameterDefinition[] buildParameterDefinitions();
}