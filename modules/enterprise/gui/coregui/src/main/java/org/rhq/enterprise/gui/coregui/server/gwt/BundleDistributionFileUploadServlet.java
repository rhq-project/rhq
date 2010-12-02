/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Accepts a "bundle distribution file" - which is simply a zip file that contains within it
 * a bundle recipe and additional bundle files.
 * 
 * @author John Mazzitelli
 */
public class BundleDistributionFileUploadServlet extends FileUploadServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void processUploadedFiles(Subject subject, Map<String, File> files, Map<String, String> formFields,
        HttpServletRequest request, HttpServletResponse response) throws IOException {

        String successMsg;

        try {
            // note that this assumes 1 and only 1 file is uploaded
            File file = files.values().iterator().next();

            BundleManagerLocal bundleManager = LookupUtil.getBundleManager();
            BundleVersion bundleVersion = bundleManager.createBundleVersionViaFile(subject, file);
            successMsg = "success [" + bundleVersion.getId() + "]";
        } catch (Exception e) {
            writeExceptionResponse(response, "Failed to upload bundle distribution file", e); // clients will look for this string!
            return;
        }

        PrintWriter writer = response.getWriter();
        writer.println("<html>");
        writer.println(successMsg);
        writer.println("</html>");
        writer.flush();
        return;
    }

}