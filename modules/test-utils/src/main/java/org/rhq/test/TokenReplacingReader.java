/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.test;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.Map;

/**
 * Copied from http://tutorials.jenkov.com/java-howto/replace-strings-in-streams-arrays-files.html
 * with fixes to {@link #read(char[], int, int)} and added support for escaping.
 *
 * @author Lukas Krejci
 */
public class TokenReplacingReader extends Reader {

    private PushbackReader pushbackReader = null;
    private Map<String, String> tokens = null;
    private StringBuilder tokenNameBuffer = new StringBuilder();
    private String tokenValue = null;
    private int tokenValueIndex = 0;
    private boolean escaping = false;
    
    public TokenReplacingReader(Reader source, Map<String, String> tokens) {
        this.pushbackReader = new PushbackReader(source, 2);
        this.tokens = tokens;
    }

    public int read(CharBuffer target) throws IOException {
        throw new RuntimeException("Operation Not Supported");
    }

    public int read() throws IOException {
        if (this.tokenValue != null) {
            if (this.tokenValueIndex < this.tokenValue.length()) {
                return this.tokenValue.charAt(this.tokenValueIndex++);
            }
            if (this.tokenValueIndex == this.tokenValue.length()) {
                this.tokenValue = null;
                this.tokenValueIndex = 0;
            }
        }

        int data = this.pushbackReader.read();
        
        if (escaping) {
            escaping = false;
            return data;
        }
        
        if (data == '\\') {
            escaping = true;
            return data;       
        }

        if (data != '$')
            return data;

        data = this.pushbackReader.read();
        if (data != '{') {
            this.pushbackReader.unread(data);
            return '$';
        }
        this.tokenNameBuffer.delete(0, this.tokenNameBuffer.length());

        data = this.pushbackReader.read();
        while (data != '}') {
            this.tokenNameBuffer.append((char) data);
            data = this.pushbackReader.read();
        }

        this.tokenValue = tokens.get(this.tokenNameBuffer.toString());

        if (this.tokenValue == null) {
            this.tokenValue = "${" + this.tokenNameBuffer.toString() + "}";
        }
        
        if (!this.tokenValue.isEmpty()) {
            return this.tokenValue.charAt(this.tokenValueIndex++);
        } else {
            return read();
        }
    }

    public int read(char cbuf[]) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    public int read(char cbuf[], int off, int len) throws IOException {
        int i = 0;
        for (; i < len; i++) {
            int nextChar = read();
            if (nextChar == -1) {
                if (i == 0) {
                    i = -1;
                }
                break;
            }
            cbuf[off + i] = (char) nextChar;
        }
        return i;
    }

    public void close() throws IOException {
        this.pushbackReader.close();
    }

    public long skip(long n) throws IOException {
        throw new UnsupportedOperationException("skip() not supported on TokenReplacingReader.");
    }

    public boolean ready() throws IOException {
        return this.pushbackReader.ready();
    }

    public boolean markSupported() {
        return false;
    }

    public void mark(int readAheadLimit) throws IOException {
        throw new IOException("mark() not supported on TokenReplacingReader.");
    }

    public void reset() throws IOException {
        throw new IOException("reset() not supported on TokenReplacingReader.");
    }
}
