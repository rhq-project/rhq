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
package org.rhq.core.util.exception;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.testng.annotations.Test;

/**
 * Tests the ability to wrap an exception so we can send it over the wire.
 *
 * @author John Mazzitelli
 */
@Test
public class WrappedRemotingExceptionTest {
    /**
     * Test when nesting exceptions.
     */
    public void testNestedWrappedRemotingExceptions() {
        Exception inner = new Exception("inner message");
        Exception mid = new Exception("mid message", inner);
        Exception outer = new Exception("outer message", mid);

        WrappedRemotingException are1 = new WrappedRemotingException(Severity.Critical, inner);
        WrappedRemotingException are2;
        WrappedRemotingException are3;

        assert are1.getCause() == null : "Should not have had any nested exceptions";
        assert are1.getMessage().equals("inner message");
        assert are1.getActualException().getSeverity().equals(Severity.Critical);

        are1 = new WrappedRemotingException(Severity.Critical, mid);
        are2 = (WrappedRemotingException) are1.getCause();
        assert are2 != null : "Should have had one nested exception";
        assert are2.getCause() == null : "Should not have had two nested exceptions";
        assert are1.getMessage().equals("mid message");
        assert are1.toString().equals("java.lang.Exception: [Critical] mid message");
        assert are2.getMessage().equals("inner message");
        assert are2.toString().equals("java.lang.Exception: [Critical] inner message");
        assert are1.getActualException().getSeverity().equals(Severity.Critical);
        assert are2.getActualException().getSeverity().equals(Severity.Critical);
        assert are1.getActualException().getStackTraceString().length() > are2.getActualException()
            .getStackTraceString().length();

        are1 = new WrappedRemotingException(Severity.Critical, outer);
        are2 = (WrappedRemotingException) are1.getCause();
        are3 = (WrappedRemotingException) are2.getCause();
        assert are2 != null : "Should have had the first of two nested exception";
        assert are3 != null : "Should have had two nested exceptions";
        assert are3.getCause() == null : "Should not have had three nested exceptions";
        assert are1.getMessage().equals("outer message");
        assert are2.getMessage().equals("mid message");
        assert are3.getMessage().equals("inner message");
        assert are1.getActualException().getSeverity().equals(Severity.Critical);
        assert are2.getActualException().getSeverity().equals(Severity.Critical);
        assert are3.getActualException().getSeverity().equals(Severity.Critical);
        assert are1.getActualException().getStackTraceString().length() > are2.getActualException()
            .getStackTraceString().length();
        assert are2.getActualException().getStackTraceString().length() > are3.getActualException()
            .getStackTraceString().length();
    }

    /**
     * Test when nesting exception packages.
     */
    public void testNestedExceptionPackages() {
        Exception inner = new Exception("inner message");
        Exception mid = new Exception("mid message", inner);
        Exception outer = new Exception("outer message", mid);

        ExceptionPackage ep1 = new ExceptionPackage(Severity.Critical, inner);
        ExceptionPackage ep2;
        ExceptionPackage ep3;

        assert ep1.getCause() == null : "Should not have had any nested exceptions";
        assert ep1.getMessage().equals("inner message");
        assert ep1.getSeverity().equals(Severity.Critical);

        ep1 = new ExceptionPackage(Severity.Critical, mid);
        ep2 = ep1.getCause();
        assert ep2 != null : "Should have had one nested exception";
        assert ep2.getCause() == null : "Should not have had two nested exceptions";
        assert ep1.getMessage().equals("mid message");
        assert ep2.getMessage().equals("inner message");
        assert ep1.getSeverity().equals(Severity.Critical);
        assert ep2.getSeverity().equals(Severity.Critical);
        assert ep1.getStackTraceString().length() > ep2.getStackTraceString().length();

        ep1 = new ExceptionPackage(Severity.Critical, outer);
        ep2 = ep1.getCause();
        ep3 = ep2.getCause();
        assert ep2 != null : "Should have had the first of two nested exception";
        assert ep3 != null : "Should have had two nested exceptions";
        assert ep3.getCause() == null : "Should not have had three nested exceptions";
        assert ep1.getMessage().equals("outer message");
        assert ep2.getMessage().equals("mid message");
        assert ep3.getMessage().equals("inner message");
        assert ep1.getSeverity().equals(Severity.Critical);
        assert ep2.getSeverity().equals(Severity.Critical);
        assert ep3.getSeverity().equals(Severity.Critical);
        assert ep1.getStackTraceString().length() > ep2.getStackTraceString().length();
        assert ep2.getStackTraceString().length() > ep3.getStackTraceString().length();
    }

    /**
     * Makes sure things are serializable.
     *
     * @throws Exception
     */
    public void testSerializable() throws Exception {
        Exception inner = new Exception("inner message");
        Exception mid = new Exception("mid message", inner);
        Exception outer = new Exception("outer message", mid);

        ExceptionPackage ep1 = new ExceptionPackage(Severity.Severe, outer);
        ExceptionPackage ep2;

        ep2 = (ExceptionPackage) deserialize(serialize(ep1));

        assert Severity.Severe.equals(ep1.getSeverity());
        assert "outer message".equals(ep1.getMessage());
        assert "java.lang.Exception".equals(ep1.getExceptionName());
        assert ep1.getSeverity().equals(ep2.getSeverity());
        assert ep1.getMessage().equals(ep2.getMessage());
        assert ep1.getExceptionName().equals(ep2.getExceptionName());
        assert ep1.getStackTraceString().equals(ep2.getStackTraceString());
        assert ep1.toString().equals(ep2.toString());

        // now test the agent remoting exception object

        WrappedRemotingException are1 = new WrappedRemotingException(Severity.Critical, outer);
        WrappedRemotingException are2;

        ep1 = are1.getActualException();
        are2 = (WrappedRemotingException) deserialize(serialize(are1));
        ep2 = are2.getActualException();

        assert Severity.Critical.equals(ep1.getSeverity());
        assert "outer message".equals(ep1.getMessage());
        assert "java.lang.Exception".equals(ep1.getExceptionName());
        assert ep1.getSeverity().equals(ep2.getSeverity());
        assert ep1.getMessage().equals(ep2.getMessage());
        assert ep1.getExceptionName().equals(ep2.getExceptionName());
        assert ep1.getStackTraceString().equals(ep2.getStackTraceString());
        assert ep1.toString().equals(ep2.toString());
    }

    /**
     * Makes sure the default severity hasn't changed.
     */
    public void testDefaultSeverity() {
        assert Severity.Warning.equals(new ExceptionPackage(null, new Exception()).getSeverity());
        assert Severity.Warning.equals(new ExceptionPackage(new Exception()).getSeverity());
        assert Severity.Warning
            .equals(new WrappedRemotingException(new Exception()).getActualException().getSeverity());
        assert Severity.Warning.equals(new WrappedRemotingException(null, new Exception()).getActualException()
            .getSeverity());

        // make sure causes have the same severity
        WrappedRemotingException are = new WrappedRemotingException(new Exception(new Exception()));
        Severity.Warning.equals(are.getActualException().getSeverity());
        Severity.Warning.equals(are.getActualException().getCause().getSeverity());
    }

    /**
     * Tests calling constructors with invalid arguments.
     */
    public void testIllegalArgumentException() {
        try {
            new ExceptionPackage(null);
            assert false : "ExceptionPackage(null) should have thrown an exception";
        } catch (IllegalArgumentException e) {
        }

        try {
            new ExceptionPackage(Severity.Severe, null);
            assert false : "ExceptionPackage(Severity, null) should have thrown an exception";
        } catch (IllegalArgumentException e) {
        }

        try {
            ExceptionPackage e = null;
            new WrappedRemotingException(e);
            assert false : "WrappedRemotingException(ExceptionPackage=null) should have thrown an exception";
        } catch (IllegalArgumentException e) {
        }

        try {
            Throwable t = null;
            new WrappedRemotingException(t);
            assert false : "WrappedRemotingException(Throwable=null) should have thrown an exception";
        } catch (IllegalArgumentException e) {
        }

        try {
            new WrappedRemotingException(Severity.Severe, null);
            assert false : "WrappedRemotingException(Severity, null) should have thrown an exception";
        } catch (IllegalArgumentException e) {
        }
    }

    /**
     * Given a serializable object, this will return the object's serialized byte array representation.
     *
     * @param  object the object to serialize
     *
     * @return the serialized bytes
     *
     * @throws Exception if failed to serialize the object
     */
    private static byte[] serialize(Serializable object) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(byteStream);
        oos.writeObject(object);
        oos.close();
        return byteStream.toByteArray();
    }

    /**
     * Deserializes the given serialization data and returns the object.
     *
     * @param  serializedData the serialized data as a byte array
     *
     * @return the deserialized object
     *
     * @throws Exception if failed to deserialize the object
     */
    private Object deserialize(byte[] serializedData) throws Exception {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(serializedData);
        ObjectInputStream ois = new ObjectInputStream(byteStream);
        Object retObject = ois.readObject();
        ois.close();
        return retObject;
    }
}