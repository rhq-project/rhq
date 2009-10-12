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
package com.jboss.jbossnetwork.product.jbpm.handlers;

import java.io.File;
import java.io.IOException;

/**
 * Utilities provided to handlers that are outside of the JBPM specific methods found in {@link BaseHandler}.
 *
 * @author Jason Dobies
 */
public class HandlerUtils {
    /**
     * Utility method to verify a parameter value has been set and is not empty. This check will throw an exception if
     * these conditions are not met.
     *
     * @param  parameterName  name of the parameter being checked
     * @param  parameterValue actual value being tested
     *
     * @throws ActionHandlerException if the value is <code>null</code> or an empty string
     */
    public static void checkIsSet(String parameterName, Object parameterValue) throws ActionHandlerException {
        if ((parameterValue == null) || "".equals(parameterValue)) {
            throw new ActionHandlerException("Parameter [" + parameterName + "] is not set.");
        }
    }

    /**
     * Formats a path for display to the user.
     *
     * @param  path in a format used by the plugin
     *
     * @return user friendly path
     */
    public static String formatPath(String path) {
        String formattedPath;
        if (path == null) {
            formattedPath = "";
        } else {
            File file = new File(path);

            if (!file.isAbsolute()) {
                formattedPath = file.getPath();
            } else {
                try {
                    formattedPath = file.getCanonicalPath();
                } catch (IOException e) {
                    // incase something goes wrong getting the canonical path, just use the absolute
                    formattedPath = file.getAbsolutePath();
                }
            }
        }

        return formattedPath;
    }

    public static String encode(byte[] bytes) {
        StringBuffer sb = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            sb.append(convertDigit((int) (bytes[i] >> 4)));
            sb.append(convertDigit((int) (bytes[i] & 0x0f)));
        }

        return (sb.toString());
    }

    public static char convertDigit(int value) {
        value &= 0x0f;
        if (value >= 10) {
            return ((char) (value - 10 + 'a'));
        } else {
            return ((char) (value + '0'));
        }
    }

    public static void checkFilenameIsWriteable(String filename) throws ActionHandlerException {
        if (!((getFile(filename)).canWrite())) {
            throw new ActionHandlerException("[" + formatPath(filename) + "] cannot be written to.");
        }
    }

    public static void checkFilenameIsReadable(String filename) throws ActionHandlerException {
        if (!((getFile(filename)).canRead())) {
            throw new ActionHandlerException("[" + formatPath(filename) + "] cannot be read from.");
        }
    }

    public static void checkFilenameIsAbsolute(String filename) throws ActionHandlerException {
        if (!((getFile(filename)).isAbsolute())) {
            throw new ActionHandlerException("[" + formatPath(filename) + "] is not an absolute path.");
        }
    }

    public static void checkFilenameIsADirectory(String filename) throws ActionHandlerException {
        if (!((getFile(filename)).isDirectory())) {
            throw new ActionHandlerException("[" + formatPath(filename) + "] is not a directory.");
        }
    }

    public static void checkFilenameIsAFile(String filename) throws ActionHandlerException {
        if (!((getFile(filename)).isFile())) {
            throw new ActionHandlerException("[" + formatPath(filename) + "] is not a file.");
        }
    }

    public static void checkFilenameExists(String filename) throws ActionHandlerException {
        if (!(getFile(filename)).exists()) {
            throw new ActionHandlerException("[" + formatPath(filename) + "] does not exist.");
        }
    }

    public static void checkFilenameIsNotEmpty(String filename) throws ActionHandlerException {
        if ((getFile(filename)).length() == 0) {
            throw new ActionHandlerException("[" + formatPath(filename) + "] is empty.");
        }
    }

    public static void checkFilenameDoesNotExist(String filename) throws ActionHandlerException {
        if ((getFile(filename)).exists()) {
            throw new ActionHandlerException("[" + formatPath(filename) + "] already exists.");
        }
    }

    private static File getFile(String filename) throws ActionHandlerException {
        return new File(filename);
    }
}