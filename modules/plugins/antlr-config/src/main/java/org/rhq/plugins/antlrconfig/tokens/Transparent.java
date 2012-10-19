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

package org.rhq.plugins.antlrconfig.tokens;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.Token;


/**
 * 
 * 
 * @author Lukas Krejci
 */
public class Transparent extends RhqConfigToken {

    private static final long serialVersionUID = 1L;

    /**
     * @param input
     * @param type
     * @param channel
     * @param start
     * @param stop
     */
    public Transparent(CharStream input, int type, int channel, int start, int stop) {
        super(input, type, channel, start, stop);
        setTransparent(true);
    }

    /**
     * @param type
     * @param text
     */
    public Transparent(int type, String text) {
        super(type, text);
        setTransparent(true);
    }

    /**
     * @param type
     */
    public Transparent(int type) {
        super(type);
        setTransparent(true);
    }

    /**
     * @param oldToken
     */
    public Transparent(Token oldToken) {
        super(oldToken);
        setTransparent(true);
    }
}
