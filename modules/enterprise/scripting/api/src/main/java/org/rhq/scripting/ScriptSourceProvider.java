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
 * <p/>
 * Note that instances of this class can be created and called in an access control context with limited privileges.
 * If you need to make safe calls that require privileges not granted to a script run in the RHQ server (by default this
 * is determined by the {@code org.rhq.bindings.StandardScriptPermissions} class), make sure to call such actions with
 * elevated permissions through
 * {@link java.security.AccessController#doPrivileged(java.security.PrivilegedExceptionAction)} or any of its
 * derivatives.
 * <p/>
 * For example JNDI look-ups are not allowed by default for the scripts, so if your provider needs to perform some
 * JNDI lookups to locate the script to include, you need to wrap any code that does a JNDI look-up as above.
 *
 * @author Lukas Krejci
 */
public interface ScriptSourceProvider {

    /**
     * Returns the reader of the source of the script specified by given location.
     * <p/>
     * Review the class description for the security considerations.
     *
     * @param location the location of the script
     * @return the reader of the script source or null if it could not be found
     */
    Reader getScriptSource(URI location);
}
