 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.communications.command.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation denotes that a remote method will be limited in the amount of concurrent calls it can handle on the
 * server. This annotation takes a string parameter which identifies the name of the "concurrency queue" that limits the
 * number of threads that can execute the method concurrently. The actual number of threads that are allowed to
 * concurrently invoke the method is configured separately on the server side. This annotation does not affect the
 * client side at all - only the server-side invocation of the remote pojo method will be affected.
 *
 * <p>This annotation must be used within an <b>interface</b>, as opposed to a class definition, in order for it to take
 * effect. In other words, when you write your remote POJO, this annotation must be used in the remote POJO's interface
 * - not its implementation class.</p>
 *
 * <p>Unlike other comm annotations, there is no Command configuration property that is analogous to this annotation. In
 * other words, you can only limit the concurrency of remote POJO invocations; you cannot limit the concurrency of raw
 * Commands sent to the server. This makes sense because we want to enforce the rules on the server-side and not rely on
 * clients to turn on concurrency controls (if we did, that would allow rogue clients to be able to circumvent the
 * concurrency controls).</p>
 *
 * @author John Mazzitelli
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LimitedConcurrency {
    /**
     * The name that identifies the "concurrency queue" that limits the number of threads that can concurrently
     * invoke this method on the server.
     *
     * @return the name identifying the concurrency queue
     */
    String value();
}