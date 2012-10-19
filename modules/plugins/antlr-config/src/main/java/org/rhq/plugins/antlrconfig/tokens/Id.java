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
 * A token in the AST can be assigned in Id class if its value represents a unique
 * identifier for the entries of the {@link Unordered} node children.
 * This enables us to track and seamlessly react to order changes in the configuration files.
 * 
 * @author Lukas Krejci
 */
public class Id extends RhqConfigToken {

    private static final long serialVersionUID = 1L;

    /**
     * @param input
     * @param type
     * @param channel
     * @param start
     * @param stop
     */
    public Id(CharStream input, int type, int channel, int start, int stop) {
        super(input, type, channel, start, stop);
    }

    /**
     * @param type
     * @param text
     */
    public Id(int type, String text) {
        super(type, text);
    }

    /**
     * @param type
     */
    public Id(int type) {
        super(type);
    }

    /**
     * @param oldToken
     */
    public Id(Token oldToken) {
        super(oldToken);
    }

    
}
