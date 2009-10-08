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
package org.rhq.enterprise.server.test;

import java.util.ArrayList;
import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

class Person {
    public Name name;
    public int age;
    public List<Phone> phone = new ArrayList<Phone>();
}

class AnotherPerson {
    public Name name;
    public int age;
    public List<Phone> phone = new ArrayList<Phone>();
}

class Name {
    public Name() {
    }

    public Name(String first, String last) {
        this.first = first;
        this.last = last;
    }

    public String first;
    public String last;
}

class Phone {
    public Phone() {
    }

    public Phone(String npa, String nxx, String number) {
        this.npa = npa;
        this.nxx = nxx;
        this.number = number;
    }

    public String npa;
    public String nxx;
    public String number;
}

class TestException extends Exception {
    public TestException() {
        super();
    }

    public TestException(Name owner) {
        super(owner.first + owner.last);
        this.owner = owner;
    }

    public Name owner;

    public static final long serialVersionUID = 0L;
}

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
public interface WebServiceTestRemote {
    @WebMethod
    String addPerson(@WebParam(name = "person")
    Person person);

    @WebMethod
    String testListArg(@WebParam(name = "list")
    List<String> list);

    @WebMethod
    AnotherPerson updatePerson(@WebParam(name = "person")
    AnotherPerson person, @WebParam(name = "name")
    Name name);

    Person getPersonByName(@WebParam(name = "name")
    Name name);

    @WebMethod
    String echo(String s);

    @WebMethod
    String hello();

    @WebMethod
    void testVoid();

    @WebMethod
    void testExceptions() throws TestException;
}