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
/*
 * OptionItem.java
 *
 * Created on February 26, 2003, 10:50 AM
 */

package org.rhq.enterprise.gui.legacy.beans;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.struts.util.LabelValueBean;
import org.rhq.core.clientapi.util.StringUtil;

/**
 * This bean is for use with html:options.
 */
public class OptionItem extends LabelValueBean implements java.io.Serializable {
    public OptionItem() {
        this(null, null);
    }

    public OptionItem(String lab, String val) {
        super(lab, val);
    }

    /**
     * Create a list of OptionItems from a list of strings.
     *
     * @param  lofs a java.util.List of Strings.
     *
     * @return A list of OptionItem objects with value and label set.
     */
    public static List createOptionsList(List lofs) {
        ArrayList newList = new ArrayList(lofs.size());
        Iterator i = lofs.iterator();
        OptionItem item;
        while (i.hasNext()) {
            String value = (String) i.next();
            String label = StringUtil.capitalize(value);
            item = new OptionItem(label, value);
            newList.add(item);
        }

        return newList;
    }
}