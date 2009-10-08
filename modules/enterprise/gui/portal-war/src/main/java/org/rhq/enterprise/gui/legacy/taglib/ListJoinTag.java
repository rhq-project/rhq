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
package org.rhq.enterprise.gui.legacy.taglib;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.taglibs.standard.tag.common.core.NullAttributeException;
import org.apache.taglibs.standard.tag.el.core.ExpressionUtil;

/**
 * A JSP tag that will take a java.util.List and format it using a given delimiter string.
 */
public class ListJoinTag extends TagSupport {
    //----------------------------------------------------instance variables

    private String list;
    private String delimiter;
    private String property;

    //----------------------------------------------------constructors

    public ListJoinTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     * Set the name of the variable in the context that holds the <code>java.util.List</code> to be formatted.
     *
     * @param list the el expression for the list variable
     */
    public void setList(String list) {
        this.list = list;
    }

    /**
     * Set the value of the list item delimiter (what will be printed between list items).
     *
     * @param delimiter the text to be printed between list items
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Set the property to be used for displaying the joined list. This is useful if the list contains objects that are
     * not primitive types, but are instead java beans.
     *
     * @param property the bean property to display
     */
    public void setProperty(String property) {
        this.property = property;
    }

    /**
     * Process the tag, generating and formatting the list.
     *
     * @exception JspException if the scripting variable can not be found or if there is an error processing the tag
     */
    public final int doStartTag() throws JspException {
        try {
            List list = (List) ExpressionUtil.evalNotNull("listJoin", "list", this.list, List.class, this, pageContext);

            String property = (String) ExpressionUtil.evalNotNull("listJoin", "property", this.property, String.class,
                this, pageContext);

            JspWriter out = pageContext.getOut();

            for (Iterator it = list.iterator(); it.hasNext();) {
                if ((null == property) || (0 == property.length())) {
                    out.write(String.valueOf(it.next()));
                } else {
                    try {
                        Object bean = it.next();
                        PropertyDescriptor pd = new PropertyDescriptor(property, bean.getClass());
                        Method m = pd.getReadMethod();
                        Object value = m.invoke(bean, null /* method args */);
                        out.write(String.valueOf(value));
                    } catch (IntrospectionException e) {
                        out.write("???" + property + "???");
                    } catch (IllegalAccessException e) {
                        out.write("???" + property + "???");
                    } catch (InvocationTargetException e) {
                        out.write("???" + property + "???");
                    }
                }

                if (it.hasNext()) {
                    out.write(delimiter);
                }
            }

            return SKIP_BODY;
        } catch (NullAttributeException e) {
            throw new JspTagException("bean " + list + " not found");
        } catch (IOException e) {
            throw new JspTagException(e.toString());
        } catch (JspException e) {
            throw new JspTagException(e.toString());
        }
    }

    public int doEndTag() throws JspException {
        release();
        return EVAL_PAGE;
    }

    public void release() {
        list = null;
        delimiter = null;
        super.release();
    }
}

// EOF
