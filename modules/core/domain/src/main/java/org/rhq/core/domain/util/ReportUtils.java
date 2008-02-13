/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Utilities for dealing with report objects returned from a plugin.
 *
 * @author Jason Dobies
 */
public abstract class ReportUtils {
    /**
     * Converts a throwable into an error message.
     *
     * @param  throwable error being converted
     *
     * @return string representation of the throwable, <code>null</code> if the throwable is <code>null</code>
     */
    public static String getErrorMessageFromThrowable(Throwable throwable) {
        if (throwable != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            throwable.printStackTrace(new PrintStream(baos));
            return baos.toString();
        } else {
            return null;
        }
    }
}