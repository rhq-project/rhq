package org.rhq.common.jbossas.client.controller;

import org.testng.annotations.Test;

@Test
public class AddressTest extends Address {
    public void testRootAddress() {
        Address addr = Address.root();
        assert addr != null;
        assert addr.equals(Address.root());
        assert addr.toString().equals("undefined");

        Address addr2 = Address.root().add("one", "two");
        assert addr2 != null;
        assert !addr2.equals(addr);
        assert addr2.getAddressNode().asList().get(0).get("one").asString().equals("two");
    }

    public void testAddress() {
        Address addr = Address.root().add("one", "two", "three", "four");
        assert addr != null;
        assert addr.getAddressNode().asList().get(0).get("one").asString().equals("two");
        assert addr.getAddressNode().asList().get(1).get("three").asString().equals("four");
    }

    public void testClone() throws CloneNotSupportedException {
        Address addr = Address.root().add("one", "two", "three", "four", "five", "six");
        Address addr2 = addr.clone();
        assert addr2 != null;
        assert addr2 != addr; // clone worked, duplicated it, didn't just return the same ref
        assert addr2.equals(addr);
        assert addr2.hashCode() == addr.hashCode();
    }
}
