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
package org.rhq.coregui.server.gwt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This servlet allows the requestor to upload a server or agent plugin file.
 *
 * @author John Mazzitelli
 */
public class PluginFileUploadServlet extends FileUploadServlet {
    private static final long serialVersionUID = 1L;

    private final Log log = LogFactory.getLog(PluginFileUploadServlet.class);

    @Override
    protected void processUploadedFiles(Subject subject, Map<String, File> files, Map<String, String> fileNames,
        Map<String, String> formFields, HttpServletRequest request, HttpServletResponse response) throws IOException {

        String successMsg;

        try {
            boolean isAllowed = LookupUtil.getAuthorizationManager().hasGlobalPermission(subject,
                Permission.MANAGE_SETTINGS);
            if (!isAllowed) {
                log.error("An unauthorized user [" + subject + "] attempted to upload a plugin");
                throw new PermissionException("You are not authorized to do this");
            }

            // note that this assumes 1 and only 1 file is uploaded
            File file = files.values().iterator().next();
            String newPluginFilename = fileNames.values().iterator().next();

            // see if it is a jar-less plugin descriptor and if not check for .jar
            if (!newPluginFilename.endsWith("-rhq-plugin.xml")) {
                // make sure its a .jar file and strip off any temp file suffix (in case the browser set the name to something odd)
                int jarExtension = newPluginFilename.lastIndexOf(".jar");
                if (jarExtension < 0) {
                    newPluginFilename = newPluginFilename + ".jar"; // make sure it ends with ".jar" in case it is some tmp filename
                    jarExtension = newPluginFilename.lastIndexOf(".jar");
                }
                newPluginFilename = newPluginFilename.substring(0, jarExtension + ".jar".length());
            }
            log.info("A new plugin [" + newPluginFilename + "] has been uploaded to [" + file + "]");

            if (file == null || !file.exists()) {
                throw new FileNotFoundException("The uploaded plugin file [" + file + "] does not exist!");
            }

            // put the new plugin file in our plugin dropbox location
            File dir = LookupUtil.getPluginManager().getPluginDropboxDirectory();
            File pluginFile = new File(dir, newPluginFilename);
            FileOutputStream fos = new FileOutputStream(pluginFile);
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    StreamUtil.copy(fis, fos);
                } finally {
                    fis.close();
                }
            } finally {
                fos.close();
            }

            log.info("A new plugin has been deployed [" + pluginFile
                + "]. A scan is required now in order to register it.");

            successMsg = "success [" + file.getName() + "]";
        } catch (Exception e) {
            writeExceptionResponse(response, "Error uploading file", e); // client looks for this exact string
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