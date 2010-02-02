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
package javax.ejb;

/**
 * This was placed into our source tree to get around the fact jboss-ejb3-all-1.0.0.Alpha9.jar
 * expects this constructor:
 * 
 *     public EJBTransactionRolledbackException(Exception ex) {
 *       super(ex);
 *   }
 *
 * but jboss-ejb3x-4.2.3.GA.jar which shows up in the classpath first
 * does not provide this implementation.
 *
 * Without this unit tests will often blow up with:
 * 
 * javax.ejb.EJBTransactionRolledbackException: method <init>(Ljava/lang/Exception;)V not found
 * 
 * and you are left scratching your head as to why it failed.  This is only used in our unit 
 * test runs.
 * 
 * @author mmccune
 *
 */
public class EJBTransactionRolledbackException extends EJBException {

    public EJBTransactionRolledbackException() {
    }

    public EJBTransactionRolledbackException(String message) {
        super(message);
    }

    public EJBTransactionRolledbackException(String message, Exception ex) {
        super(message, ex);
    }

    public EJBTransactionRolledbackException(Exception ex) {
        super(ex);
    }
}
