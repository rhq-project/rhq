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

package org.rhq.scripting;

import java.io.Reader;
import java.net.URI;

/**
 * Scripts in RHQ can be stored in various locations or maybe not even in the filesystem.
 * Implementations of this interface can be used to provide the contents of the scripts
 * based on URIs. 
 * <p>
 * Implementations of this interface can be located using the {@link ScriptSourceProviderFactory}
 * if they are registered in META-INF/services.
 * 
 * @author Lukas Krejci
 */
public interface ScriptSourceProvider {

    /**
     * Returns the reader of the source of the script specified by given location.
     * 
     * @param location the location of the script
     * @return the reader of the script source or null if it could not be found
     */
    Reader getScriptSource(URI location);
}
