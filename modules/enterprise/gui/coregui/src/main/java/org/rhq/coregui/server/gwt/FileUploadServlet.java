/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Heiko W. Rupp
 */
public class FileUploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /* The number of seconds the session can remain inactive. The default is 600 (10 minutes).
     * It may take longer than 10 minutes to upload a large file, so we'll increase...
     */
    private static final int MAX_INACTIVE_INTERVAL = 60 * 60;

    private File tmpDir;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        HttpSession session = req.getSession();
        session.setMaxInactiveInterval(MAX_INACTIVE_INTERVAL);

        if (ServletFileUpload.isMultipartContent(req)) {

            DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
            if (tmpDir == null) {
                tmpDir = LookupUtil.getCoreServer().getJBossServerTempDir();
            }
            fileItemFactory.setRepository(tmpDir);
            //fileItemFactory.setSizeThreshold(0);

            ServletFileUpload servletFileUpload = new ServletFileUpload(fileItemFactory);

            List<FileItem> fileItemsList;
            try {
                fileItemsList = servletFileUpload.parseRequest(req);
            } catch (FileUploadException e) {
                writeExceptionResponse(resp, "File upload failed", e);
                return;
            }

            List<FileItem> actualFiles = new ArrayList<FileItem>();
            Map<String, String> formFields = new HashMap<String, String>();
            boolean retrieve = false;
            boolean obfuscate = false;
            Subject authenticatedSubject = null;

            for (FileItem fileItem : fileItemsList) {
                if (fileItem.isFormField()) {
                    if (fileItem.getFieldName() != null) {
                        formFields.put(fileItem.getFieldName(), fileItem.getString());
                    }
                    if ("retrieve".equals(fileItem.getFieldName())) {
                        retrieve = true;
                    } else if ("obfuscate".equals(fileItem.getFieldName())) {
                        obfuscate = Boolean.parseBoolean(fileItem.getString());
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
                resp.setContentType("text/html");
                FileItem fileItem = actualFiles.get(0);

                ServletOutputStream outputStream = resp.getOutputStream();
                outputStream.print("<html>");
                InputStream inputStream = fileItem.getInputStream();
                try {
                    // we have to HTML escape inputStream before writing it to outputStream
                    StreamUtil.copy(inputStream, outputStream, false, true);
                } finally {
                    inputStream.close();
                }
                outputStream.print("</html>");
                outputStream.flush();

                fileItem.delete();
            } else {
                Map<String, File> allUploadedFiles = new HashMap<String, File>(); // maps form field name to the actual file
                Map<String, String> allUploadedFileNames = new HashMap<String, String>(); // maps form field name to upload file name
                for (FileItem fileItem : actualFiles) {
                    File theFile = forceToFile(fileItem);
                    if (obfuscate) {
                        // The commons fileupload API has a file tracker that deletes the file when the File object is garbage collected (huh?).
                        // Because we will be using these files later, and because they are going to be obfuscated, we don't want this to happen,
                        // so just rename the file to move it away from the file tracker and thus won't get
                        // prematurely deleted before we get a chance to use it.
                        File movedFile = new File(theFile.getAbsolutePath() + ".temp");
                        if (theFile.renameTo(movedFile)) {
                            theFile = movedFile;
                        }
                        try {
                            FileUtil.compressFile(theFile); // we really just compress it with our special compressor since its faster than obsfucation
                        } catch (Exception e) {
                            throw new ServletException("Cannot obfuscate uploaded files", e);
                        }
                    }
                    allUploadedFiles.put(fileItem.getFieldName(), theFile);
                    allUploadedFileNames.put(fileItem.getFieldName(), (fileItem.getName() != null) ? fileItem.getName()
                        : theFile.getName());
                }
                processUploadedFiles(authenticatedSubject, allUploadedFiles, allUploadedFileNames, formFields, req,
                    resp);
            }
        }
    }

    protected void writeExceptionResponse(HttpServletResponse resp, String msg, Exception e) throws IOException {
        resp.setStatus(500);
        PrintWriter writer = resp.getWriter();
        writer.write("<html><head></head><body><strong>" + msg + "</strong><br/>\n");
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        writer.write(sw.toString().replace("\n", "<br/>\n"));
        writer.write("</body></html>");
        writer.flush();
    }

    /**
     * This method will write the names of all files (the local file system location where the file was stored)
     * to the response - each local filename on its own line.
     * Subclasses are free to override this to process the files however they need.
     *
     * @param subject
     * @param files maps form field name to the actual File
     * @param fileNames maps form field name to the name of the file, as told to us by the client
     * @param formFields
     * @param request
     * @param response
     * @throws IOException
     */
    protected void processUploadedFiles(Subject subject, Map<String, File> files, Map<String, String> fileNames,
        Map<String, String> formFields, HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter writer = response.getWriter();
        writer.println("<html>");
        for (File file : files.values()) {
            String absolutePath = file.getAbsolutePath();
            writer.println(absolutePath);
        }
        writer.println("</html>");
        writer.flush();
        return;
    }

    protected File forceToFile(FileItem fileItem) throws IOException, ServletException {
        if (fileItem.isInMemory()) {
            String name = fileItem.getName();

            if (null == name) {
                throw new IllegalArgumentException("FileItem has null name");
            }

            // some browsers (IE, Chrome) pass an absolute filename, we just want the name of the file, no paths
            name = name.replace('\\', '/');
            if (name.length() > 2 && name.charAt(1) == ':') {
                name = name.substring(2);
            }
            name = new File(name).getName();

            File tmpFile = File.createTempFile(name, null);
            try {
                fileItem.write(tmpFile);
                return tmpFile;
            } catch (Exception e) {
                throw new ServletException("Failed to persist uploaded file to disk", e);
            }
        } else {
            return ((DiskFileItem) fileItem).getStoreLocation();
        }
    }
}