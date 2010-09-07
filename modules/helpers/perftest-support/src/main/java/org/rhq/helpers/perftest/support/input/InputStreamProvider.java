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

package org.rhq.helpers.perftest.support.input;

import java.io.IOException;
import java.io.InputStream;

import org.rhq.helpers.perftest.support.Input;

/**
 * A simple interface used by {@link Input} implementations to get a "fresh" copy of the
 * same input stream.
 * 
 * @author Lukas Krejci
 */
public interface InputStreamProvider {

    /**
     * @return a new input stream corresponding to the same underlying "resource" (e.g. file, URL, ...) each time.
     */
    InputStream createInputStream() throws IOException;
}
