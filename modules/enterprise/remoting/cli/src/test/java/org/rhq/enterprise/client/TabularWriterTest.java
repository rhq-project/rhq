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

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.shared.ResourceBuilder;

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

import static org.rhq.core.domain.measurement.AvailabilityType.*;

public class TabularWriterTest {

    StringWriter stringWriter;

    TabularWriter writer;

    @BeforeMethod
    public void initFixture() {
        stringWriter = new StringWriter();
        writer = new TabularWriter(new PrintWriter(stringWriter));
    }

    @Test
    public void aByteShouldPrintUnchanged() {
        byte value = 1;

        writer.print(value);

        String expected = "1\n";
        String actual = stringWriter.toString();

        assertEquals(actual, expected, "A byte or Byte should have its value printed as a String");
    }

    @Test
    public void anIntegerShouldPrintUnchanged() {
        int value = 1;

        writer.print(value);

        String expected = "1\n";
        String actual = stringWriter.toString();

        assertEquals(actual, expected, "An int or Integer should have its value printed as a String");
    }

    @Test
    public void theSimpleClassNameShouldPrintFirstForEntity() {
        User user = new User(1, "rhqadmin", "rhqadmin");

        writer.print(user);

        assertNumberOfLinesPrintedIs(4);
        assertLineEquals(0, user.getClass().getSimpleName() + ":", "The simple class name should be the first line printed");
    }

    @Test
    public void theIdShouldBeTheFirstPropertyPrintedForEntity() {
        User user = new User(1, "rhqadmin", "rhqadmin");

        writer.print(user);

        int paddingSize = "username".length();
        int idLineNumber = 1;
        String expected = "\t" + StringUtils.leftPad("id", paddingSize) + ": " + user.getId();

        assertLineEquals(idLineNumber, expected, "The id property should be the 2nd line printed");
    }

    @Test
    public void otherPropertiesShouldPrintAfterIdForEntity() {
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
    public void oneToOneAssociationShouldPrintForAnEntity() {
        User mgr = new User(1, "rhqadmin", "rhqadmin");
        Department department = new Department(1, mgr);

        writer.print(department);

        int lineNumber = 2;
        String expectedLine = "\tmanager: " + mgr;

        assertLineEquals(lineNumber, expectedLine, "The manager property should be the 3rd line printed");
    }

    @Test(enabled = false) // TODO revisit
    public void oneToManyAssociationShouldPrintForEntity() {
        User employee = new User(1, "rhq", "rhq");

        Company company = new Company(1);
        company.addEmployee(employee);

        writer.print(company);

        int lineNumber = 2;
        String expectedLine = "\temployees: " + company.getEmployees();

        assertLineEquals(lineNumber, expectedLine, "The employees property should be the 2nd line printed and the " +
                "toString() value of the collection should be displayed.");
    }

    @Test
    public void idShouldBeFirstResourcePropertyPrinted() {
        Resource resource = createResource();

        writer.print(resource);

        assertLineEquals(
            1,
            "\t" + padResourceField("id") + ": " + resource.getId(),
            "Expected Resource.id to be the first property printed."
        );
    }

    @Test
    public void nameShouldBeSecondResourcePropertyPrinted() {
        Resource resource = createResource();

        writer.print(resource);

        assertLineEquals(
            2,
            "\t" + padResourceField("name") + ": " + resource.getName(),
            "Expected Resource.name to be second property printed"
        );
    }

    @Test
    public void versionShouldBeThirdResourcePropertyPrinted() {
        Resource resource = createResource();

        writer.print(resource);

        assertLineEquals(
            3,
            "\t" + padResourceField("version") + ": " + resource.getVersion(),
            "Expected Resource.version to be third property printed"
        );
    }

    @Test
    public void currentAvailabilityShouldBeFourthResourcePropertyPrinted() {
        Resource resource = createResource();

        writer.print(resource);

        assertLineEquals(
            4,
            "\t" + padResourceField("currentAvailability") + ": " + resource.getCurrentAvailability().getAvailabilityType(),
            "Expected short version of Resource.currentAvailability to be fourth property printed"
        );
    }

    @Test
    public void handleNullCurrentAvailabilityForResource() {
        Resource resource = createUncommittedResource();

        writer.print(resource);

        assertLineEquals(
            4,
            "\t" + padResourceField("currentAvailability") + ": ",
            "Expected to see empty string for Resource.currentAvailability when property is null"
        );
    }

    @Test
    public void resourceTypeShouldBeLastResourcePropertyPrinted() {
        Resource resource = createResource();

        writer.print(resource);

        assertLineEquals(
            5,
            "\t" + padResourceField("resourceType") + ": " + resource.getResourceType().getName(),
            "Expected short version of Resource.resourceType to be the fifth property printed"
        );
    }

    private Resource createResource() {
        return new ResourceBuilder().createServer()
            .usingDefaultResourceType()
            .withId(111)
            .withName("test-server")
            .withUuid("12345")
            .withVersion("1.0")
            .inInventory()
            .withCurrentAvailability(UP)
            .build();
    }

    private Resource createUncommittedResource() {
        return new ResourceBuilder().createServer()
            .usingDefaultResourceType()
            .withId(111)
            .withName("test-server")
            .withUuid("12345")
            .withVersion("1.0")
            .notInInventory()
            .build();
    }

    @Test
    public void printCollectionOfUncommittedResource() {
        Resource parent = new ResourceBuilder().createServer()
            .usingDefaultResourceType()
            .withName("test-server")
            .withUuid("12345")
            .withVersion("1.0")
            .inInventory()
            .with(2).randomChildServices()
//                .notInInventory()
//                .included()
            .build();

        writer.print(parent.getChildResources());
    }

    void assertNumberOfLinesPrintedIs(int expectedNumberOfLines) {
        String lines[] = getLines();
        assertEquals(lines.length, expectedNumberOfLines, "The actual lines printed were\n[\n" +
            stringWriter.toString() + "\n]");
    }

    void assertLineEquals(int lineNumber, String expectedLine, String msg) {
        String actualLine = getLines()[lineNumber];

        assertEquals(actualLine, expectedLine, msg + " -- The actual output was \n[\n" + stringWriter + "\n].");
    }

    String[] getLines() {
        return stringWriter.toString().split("\n");
    }

    String padResourceField(String field) {
        return StringUtils.leftPad(field, "currentAvailability".length());
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
