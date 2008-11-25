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

import java.util.List;

import javax.ejb.Stateless;

@Stateless
// @WebService(endpointInterface = "org.rhq.enterprise.server.test.WebServiceTestRemote")
public class WebServiceTestBean implements WebServiceTestLocal, WebServiceTestRemote {
    public String addPerson(Person p) {
        String result = "person (" + p.name.first + "," + p.name.last + ") at age " + p.age + " with phone numbers (";
        for (Phone pn : p.phone) {
            result += (pn.npa + "-" + pn.nxx + "-" + pn.number);
            result += ',';
        }

        result += ") - added.";
        return result;
    }

    public AnotherPerson updatePerson(AnotherPerson p, Name n) {
        p.name = n;
        return p;
    }

    public String testListArg(List<String> list) {
        StringBuilder sb = new StringBuilder("list contains (");
        for (String item : list) {
            sb.append(item);
            sb.append(',');
        }

        return sb.toString();
    }

    public Person getPersonByName(Name n) {
        Person p = new Person();
        p.name = n;
        p.age = 22;
        p.phone.add(new Phone("919", "754", "4600"));
        p.phone.add(new Phone("919", "754", "4601"));
        p.phone.add(new Phone("919", "754", "4602"));
        return p;
    }

    public String echo(String s) {
        return s;
    }

    public String hello() {
        return "Hello, welcome to web services.";
    }

    public void testExceptions() throws TestException {
        Name n = new Name();
        n.first = "Elmer";
        n.last = "Fudd";
        throw new TestException(n);
    }

    public void testVoid() {
    }
}