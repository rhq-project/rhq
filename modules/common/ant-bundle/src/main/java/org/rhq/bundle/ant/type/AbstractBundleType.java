/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.bundle.ant.type;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.tools.ant.types.DataType;

import org.rhq.bundle.ant.BundleAntProject;
import org.rhq.core.util.file.FileUtil;

/**
 * @author Ian Springer
 */
public abstract class AbstractBundleType extends DataType {
    @Override
    public BundleAntProject getProject() {
        return (BundleAntProject) super.getProject();
    }

    protected static Pattern getPattern(List<FileSet> fileSets) {
        if (fileSets == null || fileSets.isEmpty()) {
            return null;
        }
        boolean first = true;
        StringBuilder regex = new StringBuilder();
        for (FileSet fileSet : fileSets) {
            if (!first) {
                regex.append("|");
            } else {
                first = false;
            }
            regex.append("(");
            File dir = fileSet.getDir();
            if (dir != null) {
                String path = FileUtil.useForwardSlash(dir.getPath());
                regex.append(path);
                regex.append('/');
            }
            if (fileSet.getIncludePatterns().length == 0) {
                regex.append(".*");
            } else {
                boolean firstIncludePattern = true;
                for (String includePattern : fileSet.getIncludePatterns()) {
                    if (!firstIncludePattern) {
                        regex.append("|");
                    } else {
                        firstIncludePattern = false;
                    }
                    regex.append("(");
                    buildIncludePatternRegex(includePattern, regex);
                    regex.append(")");
                }
            }
            regex.append(")");
        }
        return Pattern.compile(regex.toString());
    }

    /**
     * Builds a regex expression for a single include pattern.
     * 
     * @param includePattern the single include pattern to build a regex for
     * @param regex appends all regex characters to this regex string
     */
    protected static void buildIncludePatternRegex(String includePattern, StringBuilder regex) {
        for (int i = 0; i < includePattern.length(); i++) {
            char c = includePattern.charAt(i);
            if (c == '?') {
                regex.append('.');
            } else if (c == '*') {
                if (i + 1 < includePattern.length()) {
                    char c2 = includePattern.charAt(i + 1);
                    if (c2 == '*') {
                        regex.append(".*");
                        i += 2;
                        continue;
                    }
                }
                regex.append("[^/]*");
            } else if (c == '.') {
                regex.append("\\.");
            } else {
                regex.append(c);
            }
            // TODO: Escape backslashes.
        }
    }
}
