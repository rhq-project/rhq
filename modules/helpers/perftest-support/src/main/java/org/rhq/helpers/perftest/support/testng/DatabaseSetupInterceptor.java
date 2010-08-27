/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.testng;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.Connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.helpers.perftest.support.Importer;
import org.rhq.helpers.perftest.support.input.XmlInput;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

/**
 *
 * @author Lukas Krejci
 */
public class DatabaseSetupInterceptor implements IInvokedMethodListener {

    private static final Log LOG = LogFactory.getLog(DatabaseSetupInterceptor.class);

    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        DatabaseState state = getRequiredDatabaseState(method);
        Method connectionProviderMethod = getConnectionProviderMethod(method, state);

        try {
            InputStream dataInput = getDataInput(state.url(), state.storage(), method);
            Object classInstance = method.getTestMethod().getInstances()[0];
            Connection connection = (Connection) connectionProviderMethod.invoke(classInstance, (Object[]) null);

            XmlInput input = new XmlInput(dataInput, state.storage() == DatabaseStateStorage.FILESYSTEM);

            try {
                Importer.run(connection, input.getProducer());
            } finally {
                input.close();
            }
        } catch (Exception e) {
            LOG.warn("Failed to setup a database for method '" + method.getTestMethod().getMethodName() + "'.", e);
        }
    }

    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        //nothing to do
    }

    private static DatabaseState getRequiredDatabaseState(IInvokedMethod method) {
        Method javaMethod = method.getTestMethod().getMethod();

        return javaMethod.getAnnotation(DatabaseState.class);
    }

    private static Method getConnectionProviderMethod(IInvokedMethod method, DatabaseState state) {
        String methodName = state.connectionProviderMethod();
        Class<?> declaringClass = method.getTestMethod().getMethod().getDeclaringClass();
        if (methodName == null || methodName.trim().isEmpty()) {
            ConnectionProviderMethod methodAnnotation = declaringClass.getAnnotation(ConnectionProviderMethod.class);
            if (methodAnnotation == null) {
                throw new IllegalStateException(
                    "Neither 'connectionProviderMethod' attribute of the @DatabaseState annotation nor @ConnectionProviderMethod annotation could be found. Cannot initialize the database state without being able to get a connection.");
            }
            methodName = methodAnnotation.value();
        }
        try {
            return declaringClass.getMethod(methodName, (Class<?>[])null);
        } catch (SecurityException e) {
            LOG.warn("Failed to find a method declared by the @ConnectionProviderMethod annotation.", e);
            return null;
        } catch (NoSuchMethodException e) {
            LOG.warn("Failed to find a method declared by the @ConnectionProviderMethod annotation.", e);
            return null;
        }

    }

    private static InputStream getDataInput(String url, DatabaseStateStorage storage, IInvokedMethod method)
        throws FileNotFoundException {
        switch (storage) {
        case CLASSLOADER:
            ClassLoader cl = method.getTestMethod().getMethod().getDeclaringClass().getClassLoader();
            return cl.getResourceAsStream(url);
        case FILESYSTEM:
            return new FileInputStream(new File(url));
        default:
            return null;
        }
    }
}
