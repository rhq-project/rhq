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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Heiko W. Rupp
 */
public class FileUploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter writer;

        if (ServletFileUpload.isMultipartContent(req)) {

            DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
            //fileItemFactory.setSizeThreshold(0);

            ServletFileUpload servletFileUpload = new ServletFileUpload(fileItemFactory);

            List<FileItem> fileItemsList;
            try {
                fileItemsList = (List<FileItem>) servletFileUpload.parseRequest(req);
            } catch (FileUploadException e) {
                writer = resp.getWriter();
                writer.write("<html><head></head><body><strong>File upload failed:</strong><br/>\n");
                for (StackTraceElement elem : e.getStackTrace()) {
                    writer.write(elem.toString() + "<br/>\n");
                }
                writer.write("</body></html>");
                writer.flush();
                return;
            }

            List<FileItem> actualFiles = new ArrayList<FileItem>();
            boolean retrieve = false;
            Subject authenticatedSubject = null;

            for (FileItem fileItem : fileItemsList) {
                if (fileItem.isFormField()) {
                    if ("retrieve".equals(fileItem.getFieldName())) {
                        retrieve = true;
                    } else if ("sessionid".equals(fileItem.getFieldName())) {
                        int sessionid = Integer.parseInt(fileItem.getString());
                        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
                        try {
                            authenticatedSubject = subjectManager.getSubjectBySessionId(sessionid);
                        } catch (Exception e) {
                            throw new ServletException("Cannot authenticate request", e);
                        }
                    }
                    fileItem.delete();
                } else {
                    // file item contains an actual uploaded file
                    actualFiles.add(fileItem);
                    log("file was uploaded: " + fileItem.getName());
                }
            }

            if (authenticatedSubject == null) {
                for (FileItem fileItem : actualFiles) {
                    fileItem.delete();
                }
                throw new ServletException("Cannot process unauthenticated request");
            }

            if (retrieve && actualFiles.size() == 1) {
                // sending in "retrieve" form element with a single file means the client just wants the content echoed back
                FileItem fileItem = actualFiles.get(0);

                writer = resp.getWriter();
                writer.write("<html>");
                writer.write(new String(fileItem.get()));
                writer.write("</html>");
                writer.flush();

                fileItem.delete();
            } else {
                writer = resp.getWriter();
                writer.println("<html>");
                for (FileItem fileItem : actualFiles) {
                    String absolutePath;
                    if (fileItem.isInMemory()) {
                        // force it to disk
                        File tmpFile = File.createTempFile("" + fileItem.getName(), null);
                        try {
                            fileItem.write(tmpFile);
                        } catch (Exception e) {
                            throw new ServletException("Failed to persist uploaded file to disk", e);
                        }
                        absolutePath = tmpFile.getAbsolutePath();
                    } else {
                        absolutePath = ((DiskFileItem) fileItem).getStoreLocation().getAbsolutePath();
                    }
                    writer.println(absolutePath);
                }
                writer.println("</html>");
                writer.flush();
            }

        }

    }
}