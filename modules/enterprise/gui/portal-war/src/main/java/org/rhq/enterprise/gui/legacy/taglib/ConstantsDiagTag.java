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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.jsp.JspWriter;

/**
 * This has no utility other than for developers to see a nice dump of all of the defined constants
 */
public class ConstantsDiagTag extends ConstantsTag {
    protected void doOutput(HashMap fieldMap, JspWriter out) throws java.io.IOException {
        out.println(this.getClassname() + " has the following constants:<br>");
        out.println("<table border=\"1\"");
        for (Iterator entryIter = fieldMap.entrySet().iterator(); entryIter.hasNext();) {
            Map.Entry entry = (Map.Entry) entryIter.next();
            StringBuffer row = new StringBuffer("<tr><td>");
            row.append(entry.getKey());
            row.append("</td><td>");
            row.append(entry.getValue());
            row.append("</td></tr>");
            out.println(row.toString());
        }

        out.println("</table>");
    }
}