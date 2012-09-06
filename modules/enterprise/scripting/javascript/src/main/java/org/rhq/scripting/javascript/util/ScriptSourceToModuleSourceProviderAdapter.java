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

package org.rhq.scripting.javascript.util;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

import org.mozilla.javascript.commonjs.module.provider.ModuleSource;
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProviderBase;

import org.rhq.scripting.ScriptSourceProvider;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class ScriptSourceToModuleSourceProviderAdapter extends ModuleSourceProviderBase {
    private static final long serialVersionUID = 1L;
    
    private ScriptSourceProvider scriptSourceProvider;
    
    public ScriptSourceToModuleSourceProviderAdapter(ScriptSourceProvider provider) {
        scriptSourceProvider = provider;
    }
    
    @Override
    protected ModuleSource loadFromPrivilegedLocations(String moduleId, Object validator) throws IOException,
        URISyntaxException {

        //if the URI is absolute, we make sure to define the ModuleSource as sandboxed.
        //this is done by making the URI a "subpath" of the base.
        URI uri = new URI(moduleId);
        URI base = null;
        if (uri.isAbsolute()) {
            base = uri;
        }
        return loadFromUri(uri, base, validator);
    }

    @Override
    protected ModuleSource loadFromUri(URI uri, URI base, Object validator) throws IOException, URISyntaxException {
        URI fullUri = uri;
        if (base != null) {
            fullUri = uri.resolve(base);
        }
        
        Reader sourceReader = scriptSourceProvider.getScriptSource(fullUri);
        
        if (sourceReader == null) {
            return null;
        } else {
            return new ModuleSource(sourceReader, null, uri, base, validator);
        }
    }
}
