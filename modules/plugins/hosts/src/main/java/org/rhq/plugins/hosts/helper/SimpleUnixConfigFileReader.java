/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.hosts.helper;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * @author Ian Springer
 */
public class SimpleUnixConfigFileReader {
    private BufferedReader bufferedReader;
    private boolean trimWhitespace;

    public SimpleUnixConfigFileReader(Reader reader) {
        if (reader instanceof BufferedReader) {
            this.bufferedReader = (BufferedReader) reader;
        } else {
            this.bufferedReader = new BufferedReader(reader);
        }
    }

    public boolean isTrimWhitespace() {
        return this.trimWhitespace;
    }

    public void setTrimWhitespace(boolean trimWhitespace) {
        this.trimWhitespace = trimWhitespace;
    }

    @Nullable
    public SimpleUnixConfigFileLine readLine() throws IOException {
        String line = this.bufferedReader.readLine();
        if (line == null) {
            // end of file
            return null;
        }
        int hashIndex = line.indexOf('#');
        String comment;
        String nonComment;
        if (hashIndex != -1) {
            nonComment = line.substring(0, hashIndex);
            comment = line.substring(hashIndex + 1);
        } else {
            nonComment = line;
            comment = null;
        }
        if (this.trimWhitespace) {
            if (nonComment != null) {
                nonComment = nonComment.trim();
            }
            if (comment != null) {
                comment = comment.trim();
            }
        }
        return new SimpleUnixConfigFileLine(nonComment, comment);
    }

    public void close() throws IOException {
        this.bufferedReader.close();
    }
}
