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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.BundleFile;
import org.rhq.core.domain.content.Architecture;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This servlet allows the requestor to upload a bundle file and attach it to
 * a given BundleVersion.
 * 
 * @author John Mazzitelli
 */
public class BundleFileUploadServlet extends FileUploadServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void processUploadedFiles(Subject subject, Map<String, File> files, Map<String, String> formFields,
        HttpServletRequest request, HttpServletResponse response) throws IOException {

        String successMsg;

        try {
            // note that this assumes 1 and only 1 file is uploaded
            File file = files.values().iterator().next();

            int bundleVersionId = Integer.parseInt(getFormField(formFields, "bundleVersionId", null));
            String name = getFormField(formFields, "name", file.getName());
            String version = getFormField(formFields, "version", Integer.toString(bundleVersionId));
            Architecture architecture = new Architecture(getFormField(formFields, "arch", "noarch"));
            InputStream fileStream = new FileInputStream(file);

            BundleManagerLocal bundleManager = LookupUtil.getBundleManager();
            BundleFile bundleFile = bundleManager.addBundleFile(subject, bundleVersionId, name, version, architecture,
                fileStream);
            successMsg = "success [" + bundleFile.getId() + "]";
        } catch (Exception e) {
            writeExceptionResponse(response, "Failed to upload bundle file", e); // clients will look for this string!
            return;
        }

        PrintWriter writer = response.getWriter();
        writer.println("<html>");
        writer.println(successMsg);
        writer.println("</html>");
        writer.flush();
        return;
    }

    private String getFormField(Map<String, String> formFields, String key, String defaultValue) {
        String value = formFields.get(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

}