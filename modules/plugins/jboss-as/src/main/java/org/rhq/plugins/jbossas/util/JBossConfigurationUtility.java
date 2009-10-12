 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.jbossas.util;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;

import org.jetbrains.annotations.Nullable;

/**
 * @author Ian Springer
 */
public class JBossConfigurationUtility {
    /**
     * Returns an absolute {@link URL} representing the specified relative or absolute URL. Relative URLs are assumed to
     * represent local files and are resolved relative to the specified base directory. All file URLs are returned in
     * canonical form.
     *
     * @param urlSpec
     * @param baseDir
     * @return
     * @throws MalformedURLException
     */
    public static URL makeURL(String urlSpec, @Nullable File baseDir)
            throws MalformedURLException {
        if (baseDir != null && !baseDir.isAbsolute()) {
            throw new IllegalArgumentException("Base dir is not an absolute path: " + baseDir);
        }
        urlSpec = urlSpec.trim();
        URL url;
        try {
            url = new URL(urlSpec);
            if (url.getProtocol().equals("file")) {
                File file = (new File(url.getFile())).getCanonicalFile();
                url = file.toURL();
            }
        }
        catch (Exception e) {
            try {
                File file = new File(urlSpec);
                if (!file.isAbsolute() && baseDir != null) {
                    file = new File(baseDir, urlSpec);
                }
                file = file.getCanonicalFile();
                url = file.toURI().toURL();
            }
            catch (Exception e2) {
                throw new MalformedURLException(e2.toString());
            }
        }
        return url;
    }
}
