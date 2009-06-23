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
package org.rhq.gui.webdav;

import com.bradmcevoy.http.*;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;

/**
 * @author Greg Hinkle
 */
public abstract class BasicResource implements Resource {


    public Object authenticate(String s, String s1) {
        return "auth";  
    }

    public boolean authorise(Request request, Request.Method method, Auth auth) {
        return true;  
    }

    public String getRealm() {
        return "rhq";  
    }




 /*   public void sendContent(OutputStream out, Range range, Map<String, String> params) throws IOException {
        PrintWriter printer = new PrintWriter(out,true);
        sendContentStart(printer);
        sendContentMiddle(printer);
        sendContentFinish(printer);
    }

    protected  void sendContentMiddle(final PrintWriter printer) {
        printer.print("rename");
        printer.print("<form method='POST' action='" + this.getHref() + "'><input type='text' name='name' value='" + this.getName() + "'/><input type='submit'></form>");
    }

    protected void sendContentFinish(final PrintWriter printer) {
        printer.print("</body></html>");
        printer.flush();
    }

    protected void sendContentStart(final PrintWriter printer) {
        printer.print("<html><body>");
        printer.print("<h1>" + getName() + "</h1>");
        sendContentMenu(printer);
    }

    protected void sendContentMenu(final PrintWriter printer) {
        printer.print("<ul>");
        if (this instanceof CollectionResource) {
            for( Resource r :  ((CollectionResource)this).getChildren()) {
                printer.print("<li><a href='" + "moo" + "'>" + r.getName() + "</a>");
            }
        }
        printer.print("</ul>");
    }*/

        public String getHref() {
            return "/webdav/resource/";
        }


    public int compareTo(com.bradmcevoy.http.Resource res) {
        return this.getName().compareTo(res.getName());
    }
    
}
