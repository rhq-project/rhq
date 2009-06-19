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

package org.rhq.core.domain.util.serial;

/**
 * This uses a ThreadLocal to bind an externalization strategy based on the invoking subsystem. In other
 * words, when we know we're serializing for Server-Agent communication then set to AGENT, when we know we're
 * serializing for RemoteClient-Server communication set to REMOTEAPI.  By keeping this info on the thread
 * we avoid having to tag all of the relevant objects that will be serialized. 
 *  
 * @author jay shaughnessy
 */
public class ExternalizableStrategy {

    public enum Subsystem {
        AGENT((char) 1), REMOTEAPI((char) 2);

        private char id;

        Subsystem(char id) {
            this.id = id;
        }

        public char id() {
            return id;
        }
    }

    private static ThreadLocal<Subsystem> strategy = new ThreadLocal<Subsystem>() {

        protected ExternalizableStrategy.Subsystem initialValue() {
            return Subsystem.AGENT;
        }
    };

    public static Subsystem getStrategy() {
        return strategy.get();
    }

    public static void setStrategy(Subsystem newStrategy) {
        strategy.set(newStrategy);
    }
}
