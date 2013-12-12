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

package org.rhq.enterprise.client.script;

import java.io.File;
import java.net.URI;

/**
 * This is a configurable source provider that the users can use to load modules from 
 * a configured directory.
 *
 * @author Lukas Krejci
 */
public class ModulesDirectoryScriptSourceProvider extends FileSystemScriptSourceProvider {

    private static final String SCHEME = "modules";

    private File rootDir;

    /**
     * Creates a new instance of module script source provider that looks for the module sources in a directory
     * specified by the "rhq.scripting.modules.root-dir" system property. If none such exists, the default value
     * is assumed to be "./modules".
     */
    public ModulesDirectoryScriptSourceProvider() {
        this(new File(System.getProperty("rhq.scripting.modules.root-dir", "./modules")));
    }

    /**
     * Provided for testing purposes. A script source provider is only instantiated through its no-arg constructor
     * in the scripting environment.
     *
     * @param rootDir the root directory under which to locate module sources
     */
    public ModulesDirectoryScriptSourceProvider(File rootDir) {
        super(SCHEME);
        this.rootDir = rootDir;
    }

    public File getRootDir() {
        return rootDir;
    }

    public void setRootDir(File rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    protected File getFile(URI location) {
        String path = location.getPath();
        
        //remove the leading /
        path = path.substring(1);
        
        return new File(rootDir, path);
    }
}
