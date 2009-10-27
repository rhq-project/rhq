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
 * This is similar to {@link Unordered} but tells the {@link ConfigMapper} that
 * subtrees of the current tree are identified by their {@link Id}s but require a strict order.
 * 
 * In another words with this token type we are able to detect that the properties have been
 * reordered in the configuration and are able to reorder them accordingly in the configuration file.
 * 
 * @author Lukas Krejci
 */
public class Ordered extends RhqConfigToken {

    private static final long serialVersionUID = 1L;

    public Ordered(CharStream input, int type, int channel, int start, int stop) {
        super(input, type, channel, start, stop);
    }

    public Ordered(int type, String text) {
        super(type, text);
    }

    public Ordered(int type) {
        super(type);
    }

    public Ordered(Token oldToken) {
        super(oldToken);
    }

}
