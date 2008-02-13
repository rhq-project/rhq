package org.rhq.enterprise.gui.legacy.taglib;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.jsp.JspException;

/**
 * Define the set of columns for the "from" and "to" tables in the Add
 * To List widget. Each column is specified by a nested
 * <code>&lt;spider:addToListCol&gt;</code> tag.
 *
 * After this tag and its nested body are evaluated, a scoped
 * attribute will contain a <code>List</code> of <code>Map</code>
 * objects (each representing a column) with the following properties:
 *
 * <ul>
 *   <li> <em>name</em> - a symbolic name for the column
 *   <li> <em>key</em> - the message key for the column's table header
 * </ul>
 */
public class AddToListColsTag extends VarSetterBaseTag {

    //----------------------------------------------------instance variables

    private ArrayList cols;

    //----------------------------------------------------constructors

    public AddToListColsTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     *
     */
    public void addCol(String name, String key) {
        HashMap map = new HashMap(2);
        map.put("name", name);
        map.put("key", key);
        cols.add(map);
    }

    /**
     *
     */
    public int doStartTag() throws JspException {
        cols = new ArrayList();
        return EVAL_BODY_INCLUDE;
    }

    /**
     *
     */
    public int doEndTag() throws JspException {
        setScopedVariable(cols);
        return EVAL_PAGE;
    }

    /**
     * Release tag state.
     *
     */
    public void release() {
        cols = null;
        super.release();
    }
}
