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
package org.rhq.enterprise.gui.common.servlet;

import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.util.NumberUtil;

/**
 * <p>This class contains some utility methods for parsing parameters.</p>
 */
public abstract class ParameterizedServlet extends HttpServlet {
    // member data
    private Log log = LogFactory.getLog(ParameterizedServlet.class);

    /**
     * Parse a boolean parameter.
     *
     * @param  request      the servlet request
     * @param  paramName    the name of the parameter to parse
     * @param  defaultValue the default value for the parameter
     *
     * @return the value of the parsed parameter or the default if the parameter didn't exist
     */
    protected boolean parseBooleanParameter(HttpServletRequest request, String paramName, boolean defaultValue) {
        boolean value = defaultValue;
        String param = request.getParameter(paramName);
        if (null != param) {
            value = Boolean.valueOf(param).booleanValue();
        }

        return value;
    }

    /**
     * Parse a required boolean parameter.
     *
     * @param  request   the servlet request
     * @param  paramName the parameter name
     *
     * @return the value of the parsed parameter
     *
     * @throws IllegalArgumentException if the parameter didn't exist
     */
    protected boolean parseRequiredBooleanParameter(HttpServletRequest request, String paramName) {
        boolean value;
        String param = request.getParameter(paramName);
        if (null != param) {
            value = Boolean.valueOf(param).booleanValue();
        } else {
            throw requiredParamErr(paramName);
        }

        return value;
    }

    /**
     * Parse a double parameter.
     *
     * @param  request      the servlet request
     * @param  paramName    the name of the parameter to parse
     * @param  defaultValue the default value for the parameter
     *
     * @return the value of the parsed parameter or the default if the parameter didn't exist
     */
    protected double parseDoubleParameter(HttpServletRequest request, String paramName, double defaultValue) {
        double value = defaultValue;
        String param = request.getParameter(paramName);
        if (null != param) {
            Number n = NumberUtil.stringAsNumber(param);
            if (n.equals(NumberUtil.NaN)) {
                invalidParamWarn(paramName, param, defaultValue);
            } else {
                value = n.doubleValue();
            }
        }

        return value;
    }

    /**
     * Parse a required double parameter.
     *
     * @param  request   the servlet request
     * @param  paramName the parameter name
     *
     * @return the value of the parsed parameter
     *
     * @throws IllegalArgumentException if the parameter didn't exist
     */
    protected double parseRequiredDoubleParameter(HttpServletRequest request, String paramName) {
        double value;
        String param = request.getParameter(paramName);
        if (null != param) {
            Number n = NumberUtil.stringAsNumber(param);
            if (n.equals(NumberUtil.NaN)) {
                throw invalidParamErr(paramName, param);
            } else {
                value = n.doubleValue();
            }
        } else {
            throw requiredParamErr(paramName);
        }

        return value;
    }

    /**
     * Parse a int parameter.
     *
     * @param  request      the servlet request
     * @param  paramName    the name of the parameter to parse
     * @param  defaultValue the default value for the parameter
     *
     * @return the value of the parsed parameter or the default if the parameter didn't exist
     */
    protected int parseIntParameter(HttpServletRequest request, String paramName, int defaultValue) {
        int value = defaultValue;
        String param = request.getParameter(paramName);
        if (null != param) {
            Number n = NumberUtil.stringAsNumber(param);
            if (n.equals(NumberUtil.NaN)) {
                invalidParamWarn(paramName, param, defaultValue);
            } else {
                value = n.intValue();
            }
        }

        return value;
    }

    /**
     * Parse a required int parameter.
     *
     * @param  request   the servlet request
     * @param  paramName the parameter name
     *
     * @return the value of the parsed parameter
     *
     * @throws IllegalArgumentException if the parameter didn't exist
     */
    protected int parseRequiredIntParameter(HttpServletRequest request, String paramName) {
        int value;
        String param = request.getParameter(paramName);
        if (null != param) {
            Number n = NumberUtil.stringAsNumber(param);
            if (n.equals(NumberUtil.NaN)) {
                throw invalidParamErr(paramName, param);
            } else {
                value = n.intValue();
            }
        } else {
            throw requiredParamErr(paramName);
        }

        return value;
    }

    /**
     * Parse a long parameter.
     *
     * @param  request      the servlet request
     * @param  paramName    the name of the parameter to parse
     * @param  defaultValue the default value for the parameter
     *
     * @return the value of the parsed parameter or the default if the parameter didn't exist
     */
    protected long parseLongParameter(HttpServletRequest request, String paramName, long defaultValue) {
        long value = defaultValue;
        String param = request.getParameter(paramName);
        if (null != param) {
            Number n = NumberUtil.stringAsNumber(param);
            if (n.equals(NumberUtil.NaN)) {
                invalidParamWarn(paramName, param, defaultValue);
            } else {
                value = n.longValue();
            }
        }

        return value;
    }

    /**
     * Parse a required long parameter.
     *
     * @param  request   the servlet request
     * @param  paramName the parameter name
     *
     * @return the value of the parsed parameter
     *
     * @throws IllegalArgumentException if the parameter didn't exist
     */
    protected long parseRequiredLongParameter(HttpServletRequest request, String paramName) {
        long value;
        String param = request.getParameter(paramName);
        if (null != param) {
            Number n = NumberUtil.stringAsNumber(param);
            if (n.equals(NumberUtil.NaN)) {
                throw invalidParamErr(paramName, param);
            } else {
                value = n.longValue();
            }
        } else {
            throw requiredParamErr(paramName);
        }

        return value;
    }

    /**
     * Parse a string parameter.
     *
     * @param  request      the servlet request
     * @param  paramName    the name of the parameter to parse
     * @param  defaultValue the default value for the parameter
     *
     * @return the value of the parsed parameter or the default if the parameter didn't exist
     */
    protected String parseStringParameter(HttpServletRequest request, String paramName, String defaultValue) {
        String value = request.getParameter(paramName);
        if (null == value) {
            value = defaultValue;
        }

        return value;
    }

    /**
     * Parse the passed parameter to find out its value. If the parameter is not found, the passed default value will be
     * used
     *
     * @param  request       A HttpServletRequest
     * @param  parameterName The paramter we are looking for
     * @param  clazz         The class of the desired Enum
     * @param  defaultValue  A value of the Enum class clazz, which is taken as default
     *
     * @return either The matched value from the request or the default if it is not found or not valid
     *
     *         <p/>Usage example: <code>UnitsConstants x = parseEnumParameter(request, "unitsConstants",
     *         UnitsConstants.class, UnitsConstants.UNIT_BITS);</code>
     */
    protected <E extends Enum<E>> E parseEnumParameter(HttpServletRequest request, final String parameterName,
        Class<E> clazz, E defaultValue) {
        String param = request.getParameter(parameterName);
        if (null == param) {
            return defaultValue;
        }

        E value;
        try {
            value = Enum.valueOf(clazz, param);
        } catch (IllegalArgumentException iae) {
            value = defaultValue;
        }

        return value;
    }

    /**
     * Parse a string parameter, ensuring that it is equal to one of the passed-in <code>options</code>.
     *
     * @param  request      the servlet request
     * @param  paramName    the name of the parameter to parse
     * @param  defaultValue the default value for the parameter
     * @param  options      the list of valid values for this parameter
     *
     * @return the value of the parsed parameter or the default if the parameter didn't exist
     */
    protected String parseStringParameter(HttpServletRequest request, String paramName, String defaultValue,
        String[] options) {
        String value = request.getParameter(paramName);
        if (null != value) {
            List<String> optionList = Arrays.asList(options);
            if (!optionList.contains(value)) {
                value = defaultValue;
                if (log.isWarnEnabled()) {
                    invalidParamWarn(paramName, value, defaultValue, optionList);
                }
            }
        } else {
            value = defaultValue;
        }

        return value;
    }

    /**
     * Parse a required string parameter.
     *
     * @param  request   the servlet request
     * @param  paramName the parameter name
     *
     * @return the value of the parsed parameter
     *
     * @throws IllegalArgumentException if the parameter didn't exist
     */
    protected String parseRequiredStringParameter(HttpServletRequest request, String paramName) {
        String value = request.getParameter(paramName);
        if (null == value) {
            throw requiredParamErr(paramName);
        }

        return value;
    }

    /**
     * Parse a required string parameter, ensuring that it is equal to one of the passed-in <code>options</code>.
     *
     * @param  request   the servlet request
     * @param  paramName the parameter name
     * @param  options   the list of valid values for this parameter
     *
     * @return the value of the parsed parameter
     *
     * @throws IllegalArgumentException if the parameter didn't exist
     */
    protected String parseRequiredStringParameter(HttpServletRequest request, String paramName, String[] options) {
        String value = request.getParameter(paramName);
        if (null != value) {
            List<String> optionList = Arrays.asList(options);
            if (!optionList.contains(value)) {
                if (log.isWarnEnabled()) {
                    invalidParamErr(paramName, value, optionList);
                }
            }
        } else {
            throw requiredParamErr(paramName);
        }

        return value;
    }

    /**
     * Return an IllegalArgumentException for a required parameter.
     *
     * @param  paramName the name of the parameter
     *
     * @return an <code>IllegalArgumentException</code>
     */
    protected IllegalArgumentException requiredParamErr(String paramName) {
        String err = paramName + " is required.";
        return new IllegalArgumentException(err);
    }

    /**
     * Return an IllegalArgumentException for an invalid parameter.
     *
     * @param  paramName the name of the parameter
     * @param  param     the value of the parameter
     *
     * @return an <code>IllegalArgumentException</code>
     */
    protected IllegalArgumentException invalidParamErr(String paramName, String param) {
        String err = "invalid " + paramName + ": " + param;
        return new IllegalArgumentException(err);
    }

    /**
     * Return an IllegalArgumentException for an invalid parameter.
     *
     * @param  paramName  the name of the parameter
     * @param  param      the value of the parameter
     * @param  optionList the list of valid options for this parameter
     *
     * @return an <code>IllegalArgumentException</code>
     */
    protected IllegalArgumentException invalidParamErr(String paramName, String param, List<String> optionList) {
        String err = "invalid " + paramName + ": " + param + ", must be one of: " + optionList;
        return new IllegalArgumentException(err);
    }

    /**
     * Log a warning about an invalid parameter.
     *
     * @param paramName the name of the parameter
     * @param param     the value of the parameter
     * @param value     the default value
     */
    protected void invalidParamWarn(String paramName, String param, double value) {
        invalidParamWarn(paramName, param, String.valueOf(value));
    }

    /**
     * Log a warning about an invalid parameter.
     *
     * @param paramName the name of the parameter
     * @param param     the value of the parameter
     * @param value     the default value
     */
    protected void invalidParamWarn(String paramName, String param, int value) {
        invalidParamWarn(paramName, param, String.valueOf(value));
    }

    /**
     * Log a warning about an invalid parameter.
     *
     * @param paramName the name of the parameter
     * @param param     the value of the parameter
     * @param value     the default value
     */
    protected void invalidParamWarn(String paramName, String param, long value) {
        invalidParamWarn(paramName, param, String.valueOf(value));
    }

    /**
     * Log a warning about an invalid parameter.
     *
     * @param paramName the name of the parameter
     * @param param     the value of the parameter
     * @param value     the default value
     */
    protected void invalidParamWarn(String paramName, String param, String value) {
        if (log.isWarnEnabled()) {
            String err = "invalid " + paramName + ": " + param + ", defaulting to: " + value;
            log.warn(err);
        }
    }

    /**
     * Log a warning about an invalid parameter.
     *
     * @param paramName  the name of the parameter
     * @param param      the value of the parameter
     * @param value      the default value
     * @param optionList the list of valid options for this parameter
     */
    protected void invalidParamWarn(String paramName, String param, String value, List<String> optionList) {
        if (log.isWarnEnabled()) {
            String err = "invalid " + paramName + ": " + param + ", must be one of: " + optionList
                + ", defaulting to: " + value;
            log.warn(err);
        }
    }
}

// EOF
