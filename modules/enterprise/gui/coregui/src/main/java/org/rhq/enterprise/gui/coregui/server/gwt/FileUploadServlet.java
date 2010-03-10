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
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * @author Heiko W. Rupp
 */
public class FileUploadServlet extends HttpServlet {

//    private final Log log = LogFactory.getLog(MigrationServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter writer;
        if (ServletFileUpload.isMultipartContent(req)) {

            ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());
            List<FileItem> fileItemsList = null;
            try {
                fileItemsList = (List<FileItem>) servletFileUpload.parseRequest(req);
            } catch (FileUploadException e) {

                writer = resp.getWriter();
                writer.write("<strong>File upload failed: </strong><br/>");
                for (StackTraceElement elem : e.getStackTrace())
                    writer.write(elem.toString() + "br/>");
                writer.flush();
                return;
            }

            List<FileItem> files = new ArrayList<FileItem>();
            boolean retrieve = false;

            for (FileItem fileItem : fileItemsList) {
                if (fileItem.isFormField()) {
                    if ("retrieve".equals(fileItem.getFieldName())) {
                        retrieve = true;
                    }
                    /* The file item contains a simple name-value pair of a form field */
                    // TODO flag as error ?
                } else {
                    /* The file item contains an uploaded file */

                    files.add(fileItem);

                    System.out.println("Got the file: " + fileItem);


                }
            }


            if (retrieve && files.size() == 1) {
                writer = resp.getWriter();

                // TODO
//                InputStream s = files.get(0).getInputStream();
//                IOUtils.copy(s,writer);
                writer.write("<html>");
                writer.write(new String(files.get(0).get()));
                writer.write("</html>");
                writer.flush();

            } else {
                writer = resp.getWriter();
                writer.write("<strong>File received</strong><p/>");
                writer.flush();

            }

        }

    }
}