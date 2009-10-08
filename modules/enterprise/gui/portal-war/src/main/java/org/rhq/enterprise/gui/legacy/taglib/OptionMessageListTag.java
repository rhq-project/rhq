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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import org.apache.struts.taglib.html.Constants;
import org.apache.struts.taglib.html.OptionsCollectionTag;
import org.apache.struts.taglib.html.SelectTag;
import org.apache.struts.util.LabelValueBean;
import org.apache.struts.util.RequestUtils;

/**
 * <p>A JSP tag that will take a java.util.List and render a set of HTML <code>&lt;option ...&gt;</code> markup using
 * the resource bundle.</p>
 *
 * <p>The attributes are:
 *
 * <ul>
 *   <li><b>list</b> - the name of the list containing the key suffixes</li>
 *   <li><b>baseKey</b> - the base key in the resource bundle</li>
 * </ul>
 * </p>
 *
 * <p>This tag will look up resources using: <code>&lt;basekey&gt;.&lt;listelement&gt;.toString()</code>. Thus, if your
 * base key was <code>foo.bar.baz</code> and your list contained <code>[1, 2, 3]</code>, the resources rendered would be
 * <code>foo.bar.baz.1</code>, <code>foo.bar.baz.2</code> and <code>foo.bar.baz.3</code>.</p>
 * .
 */
public class OptionMessageListTag extends OptionsCollectionTag {
    //----------------------------------------------------instance variables

    private String bundle = org.apache.struts.Globals.MESSAGES_KEY;
    private String locale = org.apache.struts.Globals.LOCALE_KEY;
    private String baseKey;

    //----------------------------------------------------constructors

    public OptionMessageListTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     * Set the name of the resource bundle to use.
     *
     * @param list the el expression for the list variable
     */
    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    /**
     * Set the locale to use.
     *
     * @param list the el expression for the list variable
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * Set the value of the base key in the application resource bundle.
     *
     * @param delimiter the text to be printed between list items
     */
    public void setBaseKey(String baseKey) {
        this.baseKey = baseKey;
    }

    /**
     * Process the tag, generating and formatting the list.
     *
     * @exception JspException if the scripting variable can not be found or if there is an error processing the tag
     */
    public final int doStartTag() throws JspException {
        try {
            SelectTag selectTag = (SelectTag) pageContext.getAttribute(Constants.SELECT_KEY);

            Object collection = RequestUtils.lookup(pageContext, name, property, null);
            Iterator it = getIterator(collection);

            JspWriter out = pageContext.getOut();
            StringBuffer sb = new StringBuffer();
            while (it.hasNext()) {
                Object next = it.next();
                String value = null;
                String key = null;
                if (next instanceof LabelValueBean) {
                    LabelValueBean bean = (LabelValueBean) next;
                    value = bean.getValue();
                    key = baseKey + '.' + bean.getLabel();
                } else {
                    value = String.valueOf(next);
                    key = baseKey + '.' + value;
                }

                String label = RequestUtils.message(pageContext, bundle, locale, key);
                addOption(sb, label, value, selectTag.isMatched(value));
            }

            out.write(sb.toString());

            return SKIP_BODY;
        } catch (IOException e) {
            throw new JspTagException(e.toString());
        } catch (JspException e) {
            throw new JspTagException(e.toString());
        } catch (Throwable t) {
            t.printStackTrace();
            throw new JspTagException(t.toString());
        }
    }

    public int doEndTag() throws JspException {
        release();
        return EVAL_PAGE;
    }

    public void release() {
        bundle = null;
        locale = null;
        baseKey = null;
        super.release();
    }

    protected Iterator getIterator(Object collection) throws JspException {
        try {
            return super.getIterator(collection);
        } catch (ClassCastException e) {
            ArrayList list = null;
            if (collection.getClass().isArray()) {
                if (collection instanceof short[]) {
                    short[] arr = (short[]) collection;
                    list = new ArrayList(arr.length);
                    for (int i = 0; i < arr.length; ++i) {
                        list.add(new Short(arr[i]));
                    }
                } else if (collection instanceof int[]) {
                    int[] arr = (int[]) collection;
                    list = new ArrayList(arr.length);
                    for (int i = 0; i < arr.length; ++i) {
                        list.add(new Integer(arr[i]));
                    }
                } else if (collection instanceof long[]) {
                    long[] arr = (long[]) collection;
                    list = new ArrayList(arr.length);
                    for (int i = 0; i < arr.length; ++i) {
                        list.add(new Long(arr[i]));
                    }
                } else if (collection instanceof float[]) {
                    float[] arr = (float[]) collection;
                    list = new ArrayList(arr.length);
                    for (int i = 0; i < arr.length; ++i) {
                        list.add(new Float(arr[i]));
                    }
                } else if (collection instanceof double[]) {
                    double[] arr = (double[]) collection;
                    list = new ArrayList(arr.length);
                    for (int i = 0; i < arr.length; ++i) {
                        list.add(new Double(arr[i]));
                    }
                } else if (collection instanceof byte[]) {
                    byte[] arr = (byte[]) collection;
                    list = new ArrayList(arr.length);
                    for (int i = 0; i < arr.length; ++i) {
                        list.add(new Byte(arr[i]));
                    }
                } else if (collection instanceof char[]) {
                    char[] arr = (char[]) collection;
                    list = new ArrayList(arr.length);
                    for (int i = 0; i < arr.length; ++i) {
                        list.add(new Character(arr[i]));
                    }
                } else if (collection instanceof boolean[]) {
                    boolean[] arr = (boolean[]) collection;
                    list = new ArrayList(arr.length);
                    for (int i = 0; i < arr.length; ++i) {
                        list.add(new Boolean(arr[i]));
                    }
                } else {
                    list = new ArrayList();
                }
            } else {
                list = new ArrayList();
            }

            return list.iterator();
        }
    }
}

// EOF
