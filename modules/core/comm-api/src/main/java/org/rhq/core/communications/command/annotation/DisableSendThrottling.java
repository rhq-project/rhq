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
package org.rhq.core.communications.command.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation denotes that a remote method invocation (or all method invocations, if annotating a type) will ignore
 * send throttling (that is, the invocation will occur as soon as possible - it will not have to pass through the send
 * throttle first).
 *
 * <p>This annotation must be used within an <b>interface</b>, as opposed to a class definition, in order for it to take
 * effect. In other words, when you write your remote POJO, this annotation must be used in the remote POJO's interface
 * - not its implementation class.</p>
 *
 * <p>If this annotation's value is <code>false</code>, it means the method(s) are to be send-throttled - of course, the
 * communications layer must have its send throttle enabled and the send throttling parameters set appropriately,
 * otherwise, send throttling will be explicitly disabled across all remote POJO invocations regardless of this
 * annotation's value.</p>
 *
 * @author John Mazzitelli
 *
 * @see     org.rhq.enterprise.communications.command.client.ClientCommandSender#enableSendThrottling(long, long)
 * @see     org.rhq.enterprise.communications.command.client.ClientCommandSender#disableSendThrottling()
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.TYPE })
public @interface DisableSendThrottling {
    /**
     * When <code>true</code>, the method (or methods if annotating an interface) will be not be sent with send throttling enabled, in effect
     * disabling the send throttle.  If <code>false</code>, the invocations will occur with send throttling enabled.
     *
     * @return send throttling flag (default is <code>true</code>)
     *
     */
    boolean value() default true;
}