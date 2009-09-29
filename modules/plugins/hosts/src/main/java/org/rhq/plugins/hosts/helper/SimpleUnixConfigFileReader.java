/*
 * JBoss, a division of Red Hat.
 * Copyright 2006, Red Hat Middleware, LLC. All rights reserved.
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
