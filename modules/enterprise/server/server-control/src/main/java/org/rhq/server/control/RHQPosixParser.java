/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.server.control;

import java.util.ListIterator;

import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * @author Jirka Kremser
 * 
 * This parser class throws an exception if it finds some unknown option. 
 * Option in the commons-cli terminology is the command line token starting with '-' or '--')
 *
 */
public class RHQPosixParser extends PosixParser {
    private boolean ignoreUnrecognizedOption;
    
    public RHQPosixParser() {
        this(false);
    }

    public RHQPosixParser(final boolean ignoreUnrecognizedOption) {
        this.ignoreUnrecognizedOption = ignoreUnrecognizedOption;
    }

    @Override
    protected void processOption(final String arg, final ListIterator iter) throws ParseException {
        boolean hasOption = getOptions().hasOption(arg);

        if (!ignoreUnrecognizedOption && !hasOption) {
            throw new ParseException("Unknown option: " + arg);
        }
        super.processOption(arg, iter);
    }
}
