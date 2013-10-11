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
package org.rhq.coregui.client.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A static Log utility class adapter to make the GWT logging (uses jdk logging)
 * look like gwt-log (uses log4j style).
 *
 * @author Mike Thompson
 */
public class Log {

    // delegate
    private static final Logger logger = Logger.getLogger("");

    public static void config(String message) {
        logger.log(Level.CONFIG, message);
    }

    public static void fatal(String message) {
        logger.severe(message);
    }

    public static void error(String message) {
        logger.severe(message);
    }

    public static void error(String message, Throwable caught) {
        logger.log(Level.SEVERE, message, caught);
    }

    public static void warn(String message) {
        logger.warning(message);
    }

    public static void warn(String message, Throwable caught) {
        logger.log(Level.WARNING, message, caught);
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void debug(String message) {
        logger.fine(message);
    }

    public static void trace(String message) {
        logger.finest(message);
    }

    public static void trace(String message, Throwable caught) {
        logger.log(Level.FINEST, message, caught);
    }

    public static boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }


    public static boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    public static boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINEST);
    }
}
