/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.bindings.script;

import java.io.Reader;
import java.net.URI;

import org.rhq.scripting.ScriptSourceProvider;

/**
 * @author Lukas Krejci
 */
public abstract class BaseRhqSchemeScriptSourceProvider implements ScriptSourceProvider {

    public static final String SCHEME = "rhq";
    private final String authority;
    
    protected BaseRhqSchemeScriptSourceProvider(String expectedAuthority) {
        this.authority = expectedAuthority;
    }
    
    @Override
    public Reader getScriptSource(URI scriptUri) {
        if (scriptUri == null || !SCHEME.equals(scriptUri.getScheme())) {
            return null;
        }

        if (!authority.equals(scriptUri.getAuthority())) {
            return null;
        }
        
        return doGetScriptSource(scriptUri);
    }

    /**
     * Implement this method to provide the script source.
     * The base implementation of the {@link #getScriptSource(URI)} method
     * only checks that the scheme of the URI is "rhq" and that the scheme
     * specific part starts with "//".
     * <p/>
     * Please follow the general suggestions mentioned in {@link ScriptSourceProvider#getScriptSource(java.net.URI)}
     * docs.
     *
     * @param scriptUri the URI to load the script from
     * @return the reader of the script or null if the script could not be 
     * found using the URI
     * 
     * @see #getScriptSource(URI)
     */
    protected abstract Reader doGetScriptSource(URI scriptUri);
}
