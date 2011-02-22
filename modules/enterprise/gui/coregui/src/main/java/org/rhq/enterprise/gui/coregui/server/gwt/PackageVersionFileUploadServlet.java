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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Accepts a "package version file" - which is basically any file destined for use as a package version, typically
 * as backing content for a package backed resource.  The servlet will create the PackageVersion using the
 * streamed bits and, if necessary, its umbrella Package.
 * 
 * The new PackageVersion id is returned in the response. 
 * 
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 * @author Lukas Krejci
 */
public class PackageVersionFileUploadServlet extends FileUploadServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void processUploadedFiles(Subject subject, Map<String, File> files, Map<String, String> formFields,
        HttpServletRequest request, HttpServletResponse response) throws IOException {

        String successMsg;

        try {
            ContentManagerLocal contentManager = LookupUtil.getContentManager();

            // note that this assumes 1 and only 1 file is uploaded
            File file = files.values().iterator().next();

            int packageTypeId = Integer.parseInt(getFormField(formFields, "packageTypeId", null));
            String packageName = getFormField(formFields, "name", file.getName());
            packageName = new File(packageName).getName();
            String version = getFormField(formFields, "version", "0");
            String archIdField = getFormField(formFields, "archId", null);
            int architectureId = (null != archIdField) ? Integer.parseInt(archIdField) : contentManager
                .getNoArchitecture().getId();
            Integer repoId = null;
            String repoIdS = getFormField(formFields, "repoId", null);
            if (repoIdS != null) {
                repoId = Integer.parseInt(repoIdS);
            }
            
            InputStream fileStream = new FileInputStream(file);

            //use getUploadedPackageVersion instead of createPackageVersion here
            //because createPackageVersion successfully returns an already existing
            //package version with the provided "location". This is not what we want
            //here since we want to make sure that the uploaded file actually gets
            //persisted.
            Map<String, String> metaData = new HashMap<String, String>();
            metaData.put(ContentManagerLocal.UPLOAD_FILE_INSTALL_DATE, Long.toString(file.lastModified()));
            metaData.put(ContentManagerLocal.UPLOAD_FILE_NAME, packageName);
            PackageVersion packageVersion = contentManager.getUploadedPackageVersion(subject, packageName,
                packageTypeId, version, architectureId, fileStream, metaData, null, repoId);
            
            successMsg = "success [packageVersionId=" + packageVersion.getId() + ",packageId=" + packageVersion.getGeneralPackage().getId() + "]";
        } catch (Exception e) {
            writeExceptionResponse(response, "Failed to upload file", e); // clients will look for this string!
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