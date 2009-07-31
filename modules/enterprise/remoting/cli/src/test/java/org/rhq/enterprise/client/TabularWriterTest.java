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

package org.rhq.enterprise.client;

import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.apache.commons.lang.StringUtils;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.OneToMany;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;

public class TabularWriterTest {

    StringWriter stringWriter;

    TabularWriter writer;

    @BeforeMethod
    public void initFixture() {
        stringWriter = new StringWriter();
        writer = new TabularWriter(new PrintWriter(stringWriter));
    }

    @Test
    public void aByteShouldBePrintedUnchanged() {
        byte value = 1;

        writer.print(value);

        String expected = "1\n";
        String actual = stringWriter.toString();

        assertEquals(actual, expected, "A byte or Byte should have its value printed as a String");
    }

    @Test
    public void anIntegerShouldBePrintedUnchanged() {
        int value = 1;

        writer.print(value);

        String expected = "1\n";
        String actual = stringWriter.toString();

        assertEquals(actual, expected, "An int or Integer should have its value printed as a String");
    }

    @Test
    public void theSimpleClassNameShouldBePrintedFirstForAnEntity() {
        User user = new User(1, "rhqadmin", "rhqadmin");

        writer.print(user);

        assertCorrectNumberOfLinesPrinted(4);
        assertSimpleClassNameIsFirstLinePrinted(user);
    }

    @Test
    public void theIdShouldBeTheFirstPropertyPrintedForAnEntity() {
        User user = new User(1, "rhqadmin", "rhqadmin");

        writer.print(user);

        int paddingSize = "username".length();
        int idLineNumber = 1;
        String expected = "\t" + StringUtils.leftPad("id", paddingSize) + ": " + user.getId();

        assertLineEquals(idLineNumber, expected, "The id property should be the 2nd line printed");
    }

    @Test//(enabled = false)
    public void otherPropertiesShouldBePrintedAfterIdPropertyForAnEntity() {
        User user = new User(1, "rhqadmin", "rhqadmin");

        writer.print(user);

        int passwordLineNumber = 2;
        String expectedPasswordLine = "\tpassword: " + user.getPassword();

        assertLineEquals(passwordLineNumber, expectedPasswordLine, "The password property should be the 3rd line printed");

        int usernameLineNumber = 3;
        String expectedUsernameLine = "\tusername: " + user.getUsername();

        assertLineEquals(usernameLineNumber, expectedUsernameLine, "The username property should be the 4th line printed");
    }

    @Test
    public void toStringOfOneToOneAssociationShouldBePrintedForAnEntity() {
        User mgr = new User(1, "rhqadmin", "rhqadmin");
        Department department = new Department(1, mgr);

        writer.print(department);

        int lineNumber = 2;
        String expectedLine = "\tmanager: " + mgr;

        assertLineEquals(lineNumber, expectedLine, "The manager property should be the 3rd line printed");
    }

    @Test(enabled=false)
    public void toStringOfCollectionShouldBePrintedForAnEntity() {
        User employee = new User(1, "rhq", "rhq");

        Company company = new Company(1);
        company.addEmployee(employee);

        writer.print(company);

        int lineNumber = 2;
        String expectedLine = "\temployees: " + company.getEmployees();

        assertLineEquals(lineNumber, expectedLine, "The employees property should be the 2nd line printed and the " +
                "toString() value of the collection should be displayed.");
    }

    void assertCorrectNumberOfLinesPrinted(int expectedNumberOfLines) {
        String lines[] = getLines();
        assertEquals(lines.length, expectedNumberOfLines, "The actual lines printed were\n[\n" +
            stringWriter.toString() + "\n]");
    }

    void assertSimpleClassNameIsFirstLinePrinted(Object object) {
        String[] lines = getLines();
        assertEquals(lines[0], object.getClass().getSimpleName() + ":", "The simple class name should have been the " +
                "first line printed.");
    }

    void assertLineEquals(int lineNumber, String expectedLine, String msg) {
        String actualLine = getLines()[lineNumber];

        assertEquals(actualLine, expectedLine, msg + " -- The actual output was \n[\n" + stringWriter + "\n].");
    }

    String[] getLines() {
        return stringWriter.toString().split("\n");
    }

    @Entity
    static class User {
        @Id
        private int id;

        private String username;

        private String password;

        public User(int id, String username, String password) {
            this.id = id;
            this.username = username;
            this.password = password;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public String toString() {
            return User.class.getSimpleName() + "[id=" + id + ", username=" + username + ", password=" + password + "]";
        }
    }

    @Entity
    static class Department {
        @Id
        private int id;

        @OneToOne
        private User manager;

        public Department(int id, User manager) {
            this.id = id;
            this.manager = manager;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public User getManager() {
            return manager;
        }

        public void setManager(User manager) {
            this.manager = manager;
        }
    }

    @Entity
    static class Company {
        @Id
        private int id;

        @OneToMany
        private List<User> employees = new ArrayList<User>();

        public Company(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public List<User> getEmployees() {
            return employees;
        }

        public void setEmployees(List<User> employees) {
            this.employees = employees;
        }

        public void addEmployee(User employee) {
            employees.add(employee);
        }
    }

}
