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
package org.rhq.enterprise.server.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates whether or not the thread executing the method should
 * be notified via an interrupt if and when its transaction times out. If the method
 * is invoked outside the scope of any transaction, this annotation is meaningless.
 *
 * @author John Mazzitelli
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD })
public @interface InterruptOnTransactionTimeout {
    public static final boolean DEFAULT_VALUE = false;

    /**
     * When <code>true</code>, the thread executing this method will be interrupted if
     * its transaction times out.  If <code>false</code>, the thread will continue
     * without any notification if the transaction times out. 
     *
     * @return interrupt flag (default is {@link #DEFAULT_VALUE})
     *
     */
    boolean value() default DEFAULT_VALUE;
}