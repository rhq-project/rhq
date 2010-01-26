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
package org.rhq.helpers.alertMigration;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Simple servlet to upload an rhq 1.3 alert definition dump after
 * migrating the database to rhq 3.0 format.
 *
 * @author Heiko W. Rupp
 */
public class MigrationServlet extends HttpServlet {

    private final Log log = LogFactory.getLog(MigrationServlet.class);

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter writer;
        if (ServletFileUpload.isMultipartContent(req)){

            ServletFileUpload servletFileUpload = new ServletFileUpload(new DiskFileItemFactory());
            List fileItemsList = null;
            try {
                fileItemsList = servletFileUpload.parseRequest(req);
            } catch (FileUploadException e) {

                writer = resp.getWriter();
                writer.write("<strong>File upload failed: </strong><br/>");
                for (StackTraceElement elem : e.getStackTrace())
                    writer.write(elem.toString() + "br/>");
                writer.flush();
                return;
            }

            Iterator it = fileItemsList.iterator();
            while (it.hasNext()){
              FileItem fileItem = (FileItem)it.next();
              if (fileItem.isFormField()){
                /* The file item contains a simple name-value pair of a form field */
                  // TODO flag as error ?
              }
              else{
                /* The file item contains an uploaded file */

                  Alert13Parser parser = new Alert13Parser(fileItem.getInputStream());
                  List<AlertNotification> notifications = parser.parse();
                  AlertNotificationManagerLocal notificationManager = LookupUtil.getAlertNotificationManager();
                  Subject overlord = LookupUtil.getSubjectManager().getOverlord();

                  notificationManager.mergeTransientAlertNotifications(overlord,notifications);

                  writer = resp.getWriter();
                  writer.write("<strong>Alert Definitions have been migrated</strong><p/>");
                  writer.write("<a href=\"/Dashboard.do\">To the RHQ Dashboard</a>");
                  writer.flush();

              }
            }
        }

    }
}
