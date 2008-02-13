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
/**
 * Created on Mar 16, 2003
 */
package org.rhq.enterprise.gui.legacy.taglib;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.taglibs.standard.tag.common.core.Util;

/**
 * A sensible way to handle constants in a webapp is to define a bean that holds all of the constants as attributes.
 * However, Java programmers have a propensity for creating constants as classes that have the values defined static
 * final members. This tag exposes these attribute handles to save the developer from having to read the constants class
 * source code to determine the returned values. This way the JSP source and the backend bean (and/or bean handlers)
 * have a consistent interface to the constants class.
 *
 * <p/>Under the hood, the tag uses reflection to build a map of names and values. The map is cached to save the expense
 * of repeated runtime reflection.
 *
 * <p/>Usage: Suppose you have a class com.example.Constants:
 *
 * <p/>public class Constants { public static final int WARMRESTART = 0; public static final int COLDRESTART = 1; public
 * static final int HARDRESTART = 2; public static final String RO = "rock"; public static final String SHAM = "paper";
 * public static final String BO = "scissors"; }
 *
 * <p/>and you want to uniformly access the symbold names in the jsp as you would in your Java classes. Use this tag.
 *
 * <p/><%@ taglib uri="/WEB-INF/constants.tld" prefix="constants" %> ... someplace where a constant needs to be accessed:
 * <constants:constant symbol="WARMRESTART" /> Important: This usage assumes the class to access is specified in a
 * web-xml/context-param
 *
 * <p/>The alternate form <constants:constant classname="com.example.Constants" symbol="WARMRESTART" /> can be used to
 * accomadate having multiple constants classes.
 *
 * <p/>Another usage is set attributes, within an optionally specified scope <constants:constant symbol="WARMRESTART"
 * var="restartType" scope="session" /> or just <constants:constant symbol="WARMRESTART" var="restartType" /> to put it
 * in the page scope. Either way, at sometime later, you can do this <c:out value="${restartType}" />
 *
 * @author Hyperic
 * @author Ian Springer (added support for Enums)
 */
public class ConstantsTag extends TagSupport {
    private static Log log = LogFactory.getLog(ConstantsTag.class.getName());

    // This tag might handle multiple constants classes, in which
    // case we'll store them as a map. The keys are the class
    // names, the values are maps of constant names/values.
    protected static Map constants = new HashMap();

    public static final String constantsClassNameParam = "context-constants";

    // optional
    // required iff "context-constants" is not a config'd
    // param for the webapp context
    private String className = null;

    public void setClassname(String aClass) {
        className = aClass;
    }

    public String getClassname() {
        return className;
    }

    private String var = null;
    private boolean varSpecified = false;

    public void setVar(String aVar) {
        var = aVar;
        varSpecified = true;
    }

    public String getVar() {
        return var;
    }

    // if you want the page to fail if not configured, set this to true
    // by default, always fail if the Constant is not found. - tmk
    private boolean failmode = true;

    public void setFailmode(boolean theFailmode) {
        failmode = theFailmode;
    }

    public boolean getFailmode() {
        return failmode;
    }

    // required
    private String symbol = null;

    public void setSymbol(String aSymbol) {
        symbol = aSymbol;
    }

    public String getSymbol() {
        return symbol;
    }

    private String scopeName;
    private int scope = PageContext.PAGE_SCOPE;
    private boolean scopeSpecified = false;

    public void setScope(String aScopeName) {
        scopeName = aScopeName;
        scope = Util.getScope(aScopeName);
        scopeSpecified = true;
    }

    public String getScope() {
        return scopeName;
    }

    public void release() {
        super.release();
        className = symbol = var = scopeName = null;
        failmode = false;
        failmode = scopeSpecified = false;
        scope = PageContext.PAGE_SCOPE;
    }

    public int doEndTag() throws JspException {
        try {
            JspWriter out = pageContext.getOut();
            if (className == null) {
                className = pageContext.getServletContext().getInitParameter(constantsClassNameParam);
            }

            if (validate(out)) {
                // we're misconfigured. getting this far
                // is a matter of what our failure mode is;
                // if we haven't thrown an Error, carry on
                log.debug("constants tag misconfigured");
                return EVAL_PAGE;
            }

            Map<String, String> fieldMap;
            if (constants.containsKey(className)) {
                // we cache the result of the constant's class
                // reflection field walk as a map
                fieldMap = (Map<String, String>) constants.get(className);
            } else {
                fieldMap = new HashMap<String, String>();
                Class typeClass = Class.forName(className);
                if (typeClass.isEnum()) {
                    for (Object enumConstantObj : typeClass.getEnumConstants()) {
                        Enum enumConstant = (Enum) enumConstantObj;

                        // Set name *and* value to enum name (e.g. name of ResourceCategory.PLATFORM = "PLATFORM")
                        // NOTE: We do not set the value to enumConstant.ordinal(), because there is no way to
                        // convert the ordinal value back to an Enum (i.e. no Enum.valueOf(int ordinal) method).
                        fieldMap.put(enumConstant.name(), enumConstant.name());
                    }
                } else {
                    Object instance = typeClass.newInstance();
                    Field[] fields = typeClass.getFields();
                    for (Field field : fields) {
                        // string comparisons of class names should be cheaper
                        // than reflective Class comparisons, the asumption here
                        // is that most constants are Strings, ints and booleans
                        // but a minimal effort is made to accomadate all types
                        // and represent them as String's for our tag's output
                        String fieldType = field.getType().getName();
                        String strVal;
                        if (fieldType.equals("java.lang.String")) {
                            strVal = (String) field.get(instance);
                        } else if (fieldType.equals("int")) {
                            strVal = Integer.toString(field.getInt(instance));
                        } else if (fieldType.equals("boolean")) {
                            strVal = Boolean.toString(field.getBoolean(instance));
                        } else if (fieldType.equals("char")) {
                            strVal = Character.toString(field.getChar(instance));
                        } else if (fieldType.equals("double")) {
                            strVal = Double.toString(field.getDouble(instance));
                        } else if (fieldType.equals("float")) {
                            strVal = Float.toString(field.getFloat(instance));
                        } else if (fieldType.equals("long")) {
                            strVal = Long.toString(field.getLong(instance));
                        } else if (fieldType.equals("short")) {
                            strVal = Short.toString(field.getShort(instance));
                        } else if (fieldType.equals("byte")) {
                            strVal = Byte.toString(field.getByte(instance));
                        } else {
                            strVal = field.get(instance).toString();
                        }

                        fieldMap.put(field.getName(), strVal);
                    }
                }

                // cache the result
                constants.put(className, fieldMap);
            }

            if ((symbol != null) && !fieldMap.containsKey(symbol)) {
                // tell the developer that he's being a dummy and what
                // might be done to remedy the situation
                // TODO: what happens if the constants change?
                // do we need to throw a JspException, here? - mtk
                String err1 = symbol + " was not found in " + className + "\n";
                String err2 = err1 + "use <constants:diag classname=\"" + className + "\"/>\n"
                    + "to figure out what you're looking for";
                log.error(err2);
                die(out, err1);
            }

            if (varSpecified) {
                doSet(fieldMap);
            } else {
                doOutput(fieldMap, out);
            }
        } catch (JspException e) {
            throw e;
        } catch (Exception e) {
            log.debug("doEndTag() failed: ", e);
            throw new JspException("Could not access constants tag", e);
        }

        return EVAL_PAGE;
    }

    protected void doOutput(Map fieldMap, JspWriter out) throws java.io.IOException {
        out.print(fieldMap.get(symbol));
    }

    protected void doSet(Map fieldMap) {
        pageContext.setAttribute(var, fieldMap.get(symbol), scope);
    }

    /**
     * Checks for broken configuration/attribute combinations.
     *
     * @return true is validation fails
     */
    protected boolean validate(JspWriter out) throws JspException {
        // look for configuration errors
        if (className == null) {
            // the tag has to be parameterized either at tag
            // invocation time or as a web.xml
            // web-app/context-param element (which takes
            // param-name and param-value subelements)
            // if it's null here, it's busted
            die(out);
            return true;
        }

        if ((scopeName != null) && (var == null)) {
            // setting the scope but not a var to assign to
            // is a misconfiguration
            die(out);
            return true;
        }

        return false;
    }

    protected void die(JspWriter out) throws JspException {
        die(out, "");
    }

    protected void die(JspWriter out, String err) throws JspException {
        if (failmode) {
            throw new JspException(err);
        } else {
            try {
                out.println("<!-- constants tag misconfigured -->");
            } catch (java.io.IOException e) {
                // misconfigured and hosed, what a difficult
                // situation
                log.debug("constants tag misconfigured: ", e);
            }
        }
    }
}