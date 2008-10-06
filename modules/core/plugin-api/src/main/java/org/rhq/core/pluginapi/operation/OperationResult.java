 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.pluginapi.operation;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * An object that encapsulates the results of a single operation invocation. Since this class contains a
 * {@link #getComplexResults() Configuration object}, it can be used to store complex results. However, if an operation
 * needs to only return a single string result, use the convenience constructor
 * {@link OperationResult#OperationResult(String)} or the method {@link #setSimpleResult(String)} to pass in that simple
 * string value without the need to use the more complex {@link Configuration} API.
 *
 * @author John Mazzitelli
 * @see    Configuration
 * @see    OperationFacet
 */
public class OperationResult {
    public static final String SIMPLE_OPERATION_RESULT_NAME = "operationResult";

    private final Configuration complexResults = new Configuration();

    /**
     * Constructor that builds an empty result. You can then use {@link #getComplexResults()} to obtain the
     * {@link Configuration} object in order to populate it with complex results returned by the operation. You are also
     * free to store a simple result by using {@link #setSimpleResult(String)}. Typically, you will normally want to
     * store either a simple result or a complex result, but not both (although technically you are not prohibited from
     * using both).
     *
     * @see #getComplexResults()
     */
    public OperationResult() {
    }

    /**
     * Convenience constructor that builds a result that contains a single, simple result string. You can technically
     * still use {@link #getComplexResults()} to obtain the {@link Configuration} object in order to populate it with
     * additional, complex results returned by the operation but, typically, callers that use this constructor only need
     * to store a single result string in the results.
     *
     * @param simpleResult the single, simple result string to store in the results object
     *
     * @see   #getSimpleResult()
     */
    public OperationResult(String simpleResult) {
        setSimpleResult(simpleResult);
    }

    /**
     * Gets the simple string result that was stored in this object.
     *
     * @return the simple string result
     *
     * @see    #OperationResult(String)
     * @see    #setSimpleResult(String)
     */
    public String getSimpleResult() {
        return complexResults.getSimpleValue(SIMPLE_OPERATION_RESULT_NAME, null);
    }

    /**
     * Use this method if an operation returned a simple string result. This is a convenience method that places the
     * given result string in the {@link #getComplexResults()} object under the name
     * {@link #SIMPLE_OPERATION_RESULT_NAME}.
     *
     * @param simpleResult a simple string result
     */
    public void setSimpleResult(String simpleResult) {
        complexResults.put(new PropertySimple(SIMPLE_OPERATION_RESULT_NAME, simpleResult));
    }

    /**
     * Returns the {@link Configuration} object that is used to contain all the complex data that resulted from an
     * operation invocation. The returned object is not a copy, so you can use this object to populate the complex
     * results.
     *
     * <p>Note that this is the same object that will be populated by the convenience method
     * {@link #setSimpleResult(String)}.</p>
     *
     * @return the object that will contain the complex results
     */
    public Configuration getComplexResults() {
        return complexResults;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + complexResults + "]";
    }
}