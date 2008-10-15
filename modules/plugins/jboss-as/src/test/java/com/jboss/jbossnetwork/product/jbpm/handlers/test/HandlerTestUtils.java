 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package com.jboss.jbossnetwork.product.jbpm.handlers.test;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

/**
 * @author Jason Dobies
 */
public class HandlerTestUtils {
    /**
     * Loads the JBPM process at the specified file name (using the class loader) and substitutes in the specified
     * parameters.
     *
     * @param  filename   JBPM process file
     * @param  parameters to be substituted into the workflow
     *
     * @return populated process
     *
     * @throws IOException if the file cannot be read
     */
    public static String getProcessAsString(String filename, String[] parameters) throws IOException {
        InputStream processStream = HandlerTestUtils.class.getClassLoader().getResourceAsStream(filename);

        byte[] b = new byte[processStream.available()];
        processStream.read(b);
        String processString = new String(b);

        String finalProcess = MessageFormat.format(processString, parameters);

        return finalProcess;
    }

    /**
     * Writes out a file containing a small amount of text to the specified file location.
     *
     * @param  filename destination to write the file to
     *
     * @throws IOException if there is an error writing the file
     */
    public static void createSampleFile(String filename) throws IOException {
        FileOutputStream fos = new FileOutputStream(filename);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        String contents = "Sample file located at " + filename;

        bos.write(contents.getBytes());

        bos.close();
    }
}