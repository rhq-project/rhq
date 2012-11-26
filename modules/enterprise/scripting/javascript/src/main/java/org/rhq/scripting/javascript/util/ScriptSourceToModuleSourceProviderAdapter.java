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
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider;
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProviderBase;

import org.rhq.scripting.ScriptSourceProvider;
import org.rhq.scripting.javascript.JsEngineProvider;

/**
 * This is an adapter that acts as a {@link ModuleSourceProvider} for Rhino
 * but uses RHQ's {@link ScriptSourceProvider} to load the scripts.
 * 
 * @author Lukas Krejci
 */
public class ScriptSourceToModuleSourceProviderAdapter extends ModuleSourceProviderBase {
    private static final long serialVersionUID = 1L;

    private static final String SUFFIX = "." + JsEngineProvider.SCRIPT_FILE_EXTENSION;

    private ScriptSourceProvider scriptSourceProvider;

    public ScriptSourceToModuleSourceProviderAdapter(ScriptSourceProvider provider) {
        scriptSourceProvider = provider;
    }

    @Override
    protected ModuleSource loadFromPrivilegedLocations(String moduleId, Object validator) throws IOException,
        URISyntaxException {

        URI uri = new URI(moduleId);
        if (!uri.isAbsolute()) {
            return null;
        }
        return loadFromUri(uri, null, validator);
    }

    @Override
    protected ModuleSource loadFromUri(URI uri, URI base, Object validator) throws IOException, URISyntaxException {
        URI fullUri = uri;
        if (base != null) {
            fullUri = base.resolve(uri);
        }

        if (!fullUri.getSchemeSpecificPart().endsWith(SUFFIX)) {            
            fullUri =
                new URI(fullUri.getScheme(), fullUri.getAuthority(), fullUri.getPath() + SUFFIX, fullUri.getQuery(),
                    fullUri.getFragment());
        }
        
        Reader sourceReader = scriptSourceProvider.getScriptSource(fullUri);

        if (sourceReader == null) {
            return null;
        } else {
            return new ModuleSource(sourceReader, null, uri, base, validator);
        }
    }
}
