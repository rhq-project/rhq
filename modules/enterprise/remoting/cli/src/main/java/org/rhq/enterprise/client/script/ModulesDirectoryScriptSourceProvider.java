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

    private static final File ROOT_DIR = new File(System.getProperty("rhq.scripting.modules.root-dir", "./samples/modules"));
    private static final String SCHEME = "modules";
    
    public ModulesDirectoryScriptSourceProvider() {
        super(SCHEME);
    }
    
    @Override
    protected File getFile(URI location) {
        String path = location.getPath();
        
        //remove the leading /
        path = path.substring(1);
        
        return new File(ROOT_DIR, path);
    }
}
