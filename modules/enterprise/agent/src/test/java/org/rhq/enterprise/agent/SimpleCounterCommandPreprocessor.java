/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.agent;

import java.util.concurrent.atomic.AtomicLong;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.CommandPreprocessor;

/**
 * A preprocessor our tests can use that simply counts the number of commands it preprocesses. This assumes only one
 * instance will ever be created/used at any one time.
 *
 * @author John Mazzitelli
 */
public class SimpleCounterCommandPreprocessor implements CommandPreprocessor {
    /**
     * The counter; its public static so tests can directly access it without having a reference to the current
     * instance. There is currently no API to get the preprocessors, so we need this to be a static.
     */
    public static final AtomicLong COUNTER = new AtomicLong(0L);

    /**
     * Constructor for {@link SimpleCounterCommandPreprocessor}.
     */
    public SimpleCounterCommandPreprocessor() {
        COUNTER.set(0L);
    }

    /**
     * @see CommandPreprocessor#preprocess(Command, ClientCommandSender)
     */
    public void preprocess(Command command, ClientCommandSender sender) {
        COUNTER.incrementAndGet();
    }
}