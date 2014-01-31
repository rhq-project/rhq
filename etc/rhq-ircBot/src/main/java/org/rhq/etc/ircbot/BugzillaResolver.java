/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.etc.ircbot;

import com.j2bugzilla.base.Bug;
import com.j2bugzilla.base.BugzillaConnector;
import com.j2bugzilla.base.BugzillaException;
import com.j2bugzilla.base.ConnectionException;
import com.j2bugzilla.rpc.GetBug;

import org.apache.xmlrpc.XmlRpcException;

/**
 * @author Jirka Kremser
 *
 */
public class BugzillaResolver implements BugResolver {
    
    private BugzillaConnector bzConnector = new BugzillaConnector();

    @Override
    public String resolve(String bugIdentifier) {
        int bugId = Integer.valueOf(bugIdentifier);
        GetBug getBug = new GetBug(bugId);
        try {
            bzConnector.executeMethod(getBug);
        } catch (Exception e) {
            bzConnector = new BugzillaConnector();
            try {
                bzConnector.connectTo("https://bugzilla.redhat.com");
            } catch (ConnectionException e2) {
                e2.printStackTrace();
                return "Failed to access BZ " + bugId + ": " + e2.getMessage();
            }
            try {
                bzConnector.executeMethod(getBug);
            } catch (BugzillaException e1) {
                //e1.printStackTrace();
                Throwable cause = e1.getCause();
                String details = (cause instanceof XmlRpcException) ? cause.getMessage() : e1.getMessage();
                return "Failed to access BZ " + bugId + ": " + details;
            }
        }
        Bug bug = getBug.getBug();
        if (bug != null) {
            String product = bug.getProduct();
            if (product.equals("RHQ Project")) {
                product = "RHQ";
            } else if (product.equals("JBoss Operations Network")) {
                product = "JON";
            }
            return "BZ " + bugId + " [product=" + product + ", priority=" + Color.GREEN + bug.getPriority()
                + Color.NORMAL + ", status=" + bug.getStatus() + "] " + Color.RED + bug.getSummary() + Color.NORMAL
                + " [ https://bugzilla.redhat.com/" + bugId + " ]";
        } else {
            return ("BZ " + bugId + " does not exist.");
        }
    }
}
