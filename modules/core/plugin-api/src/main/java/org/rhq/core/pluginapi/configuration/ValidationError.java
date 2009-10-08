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
package org.rhq.core.pluginapi.configuration;

import org.rhq.core.util.exception.ExceptionPackage;

/**
 * Encapsulates a configuration error that was caused by an invalid configuration value.
 *
 * @author John Mazzitelli
 */
public class ValidationError {
    private String propertyName;
    private String invalidValue;
    private ExceptionPackage error;

    public ValidationError(String propertyName, String invalidValue, ExceptionPackage error) {
        this.propertyName = propertyName;
        this.invalidValue = invalidValue;
        this.error = error;
    }

    public ValidationError(String propertyName, String invalidValue, String errorMessage) {
        this(propertyName, invalidValue, new ExceptionPackage(new Exception(errorMessage)));
    }

    /**
     * Returns the name of the property whose value did not pass the validation tests.
     *
     * @return property with the invalid value
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * The value that did not pass the validation tests.
     *
     * @return the invalid value, in its string form
     */
    public String getInvalidValue() {
        return invalidValue;
    }

    /**
     * The error that indicates how or why the value was invalid.
     *
     * @return validation error message
     */
    public ExceptionPackage getError() {
        return error;
    }

    public String toString() {
        StringBuilder str = new StringBuilder("ValidationError: [");
        str.append("property-name=[" + this.propertyName);
        str.append("], invalid-value=[" + this.invalidValue);
        str.append("], error=[" + this.error);
        str.append("]");
        return str.toString();
    }
}