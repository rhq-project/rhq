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
 * This annotation denotes that a remote method invocation (or all method invocations, if annotating a type) will be
 * sent asynchronously. You can explicitly turn off asynchronous mode by setting the value attribute to <code>
 * false</code>. You can optionally define if the invocation is to have guaranteed delivery or not.
 *
 * <p>This annotation must be used within an <b>interface</b>, as opposed to a class definition, in order for it to take
 * effect. In other words, when you write your remote POJO, this annotation must be used in the remote POJO's interface
 * - not its implementation class.</p>
 *
 * @author John Mazzitelli
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.TYPE })
public @interface Asynchronous {
    /**
     * When <code>true</code>, the method (or methods if annotating an interface) will be sent asynchronously.
     *
     * @return asynchronous flag (default is <code>true</code>)
     */
    boolean value() default true;

    /**
     * When <code>true</code>, the asynchronous invocation will have guaranteed delivery enabled meaning
     * that a best effort will be made to invoke the method, even in the event of a network or server/client crash.
     *
     * @return guaranteed delivery flag (default is <code>false</code>)
     */
    boolean guaranteedDelivery() default false;
}