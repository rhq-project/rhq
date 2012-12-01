/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.test.ldap;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;
import javax.naming.ldap.LdapContext;

import org.rhq.enterprise.server.resource.group.LDAPStringUtil;

/**
 * A fake implementation of {@link LdapContext} which can be used for LDAP 
 * unit testing.
 *  
 * @author loleary
 *
 */
public class FakeLdapContext implements LdapContext {

    FakeLdapContext.LdapSearchTestData ldapTestData = new LdapSearchTestData();

    @Override
    public NamingEnumeration<SearchResult> search(String name, String filter, SearchControls cons)
        throws NamingException {
        Hashtable<String, String> attributes = new Hashtable<String, String>();

        if (name != null && name.length() > 0) {
            attributes.put("baseName", name);
        }

        /*
         *  simple parse of filter
         *  We treat everything like an &
         *  We do not handle wildcards
         */
        if (filter != null) {
            int idxLP = -1; // Left Parenthesis
            int idxEq = -1; // Equal sign
            int idxRP = -1; // Right Parenthesis
            while (idxLP < filter.length() && idxRP < filter.length()) {
                idxLP = filter.indexOf('(', idxLP + 1);
                if (idxLP > -1 && idxLP + 1 < filter.length()) {
                    if (filter.charAt(idxLP + 1) == '&') {
                        idxLP++;
                        continue;
                    }
                    idxEq = filter.indexOf('=', idxLP + 1);
                }
                if (idxEq > -1 && idxEq + 1 < filter.length())
                    idxRP = filter.indexOf(')', idxEq + 1);
                if (idxLP > -1 && idxEq > idxLP && idxRP > idxEq) {
                    String attrName = filter.substring(idxLP + 1, idxEq);
                    String attrValue = filter.substring(idxEq + 1, idxRP);
                    attributes.put(attrName, attrValue);
                } else {
                    idxRP = filter.length();
                }
            }
        }

        try {
            return new FakeNamingEnumeration<SearchResult>(ldapTestData.getSearchResults(attributes));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public FakeLdapContext() throws NamingException {
        super();
        //System.out.println("------------------------------------------------");
        //System.out.println(FakeLdapContext.class.getCanonicalName() + " initialized");
        //System.out.println("------------------------------------------------");
    }

    @Override
    public void bind(Name arg0, Object arg1, Attributes arg2) throws NamingException {
    }

    @Override
    public void bind(String arg0, Object arg1, Attributes arg2) throws NamingException {
    }

    @Override
    public DirContext createSubcontext(Name arg0, Attributes arg1) throws NamingException {
        return null;
    }

    @Override
    public DirContext createSubcontext(String arg0, Attributes arg1) throws NamingException {
        return null;
    }

    @Override
    public Attributes getAttributes(Name arg0) throws NamingException {
        return null;
    }

    @Override
    public Attributes getAttributes(String arg0) throws NamingException {
        return null;
    }

    @Override
    public Attributes getAttributes(Name arg0, String[] arg1) throws NamingException {
        return null;
    }

    @Override
    public Attributes getAttributes(String arg0, String[] arg1) throws NamingException {
        return null;
    }

    @Override
    public DirContext getSchema(Name arg0) throws NamingException {
        return null;
    }

    @Override
    public DirContext getSchema(String arg0) throws NamingException {
        return null;
    }

    @Override
    public DirContext getSchemaClassDefinition(Name arg0) throws NamingException {
        return null;
    }

    @Override
    public DirContext getSchemaClassDefinition(String arg0) throws NamingException {
        return null;
    }

    @Override
    public void modifyAttributes(Name arg0, ModificationItem[] arg1) throws NamingException {
    }

    @Override
    public void modifyAttributes(String arg0, ModificationItem[] arg1) throws NamingException {
    }

    @Override
    public void modifyAttributes(Name arg0, int arg1, Attributes arg2) throws NamingException {
    }

    @Override
    public void modifyAttributes(String arg0, int arg1, Attributes arg2) throws NamingException {
    }

    @Override
    public void rebind(Name arg0, Object arg1, Attributes arg2) throws NamingException {
    }

    @Override
    public void rebind(String arg0, Object arg1, Attributes arg2) throws NamingException {
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name arg0, Attributes arg1) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String arg0, Attributes arg1) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name arg0, Attributes arg1, String[] arg2) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String arg0, Attributes arg1, String[] arg2) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name arg0, String arg1, SearchControls arg2) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(Name arg0, String arg1, Object[] arg2, SearchControls arg3)
        throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<SearchResult> search(String arg0, String arg1, Object[] arg2, SearchControls arg3)
        throws NamingException {
        return null;
    }

    @Override
    public Object addToEnvironment(String arg0, Object arg1) throws NamingException {
        return null;
    }

    @Override
    public void bind(Name arg0, Object arg1) throws NamingException {
    }

    @Override
    public void bind(String arg0, Object arg1) throws NamingException {
    }

    @Override
    public void close() throws NamingException {

    }

    @Override
    public Name composeName(Name arg0, Name arg1) throws NamingException {
        return null;
    }

    @Override
    public String composeName(String arg0, String arg1) throws NamingException {
        return null;
    }

    @Override
    public Context createSubcontext(Name arg0) throws NamingException {
        return null;
    }

    @Override
    public Context createSubcontext(String arg0) throws NamingException {
        return null;
    }

    @Override
    public void destroySubcontext(Name arg0) throws NamingException {
    }

    @Override
    public void destroySubcontext(String arg0) throws NamingException {
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        return null;
    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return null;
    }

    @Override
    public NameParser getNameParser(Name arg0) throws NamingException {
        return null;
    }

    @Override
    public NameParser getNameParser(String arg0) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name arg0) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String arg0) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name arg0) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String arg0) throws NamingException {
        return null;
    }

    @Override
    public Object lookup(Name arg0) throws NamingException {
        return null;
    }

    @Override
    public Object lookup(String arg0) throws NamingException {
        return null;
    }

    @Override
    public Object lookupLink(Name arg0) throws NamingException {
        return null;
    }

    @Override
    public Object lookupLink(String arg0) throws NamingException {
        return null;
    }

    @Override
    public void rebind(Name arg0, Object arg1) throws NamingException {
    }

    @Override
    public void rebind(String arg0, Object arg1) throws NamingException {
    }

    @Override
    public Object removeFromEnvironment(String arg0) throws NamingException {
        return null;
    }

    @Override
    public void rename(Name arg0, Name arg1) throws NamingException {
    }

    @Override
    public void rename(String arg0, String arg1) throws NamingException {
    }

    @Override
    public void unbind(Name arg0) throws NamingException {
    }

    @Override
    public void unbind(String arg0) throws NamingException {
    }

    @Override
    public ExtendedResponse extendedOperation(ExtendedRequest arg0) throws NamingException {
        return null;
    }

    @Override
    public Control[] getConnectControls() throws NamingException {
        return null;
    }

    @Override
    public Control[] getRequestControls() throws NamingException {
        return null;
    }

    @Override
    public Control[] getResponseControls() throws NamingException {
        return null;
    }

    @Override
    public LdapContext newInstance(Control[] arg0) throws NamingException {
        return null;
    }

    @Override
    public void reconnect(Control[] arg0) throws NamingException {
    }

    @Override
    public void setRequestControls(Control[] arg0) throws NamingException {
    }

    /**
     * Test data that is returned as LDAP search results based on simple queries.
     * 
     * @author loleary
     *
     */
    public class LdapSearchTestData extends ArrayList<SearchResult> {

        private static final long serialVersionUID = -1751409714032152844L;

        public LdapSearchTestData() {
            super();
            Attribute attr = null;
            Attributes attrs = null;
            SearchResult sr = null;

            // dn: dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("dcObject");
            attr.add("organization");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("dc");
            attr.add("test-rhq");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("Test directory for RHQ from Red Hat, Inc. Directory should contain entries that can be used to test LDAP integration between RHQ Server and a compatible LDAP server implementation. ");
            attrs.put(attr);

            attr = new BasicAttribute("o");
            attr.add("Test RHQ directory");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("dc=test,dc=rhq,dc=redhat,dc=com", null, null, attrs, true);
            this.add(sr);

            // dn: ou=groups,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalUnit");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("groups");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("All groups which can be used for testing RHQ role/group mappings.");
            attrs.put(attr);

            sr = new SearchResult("groups", null, null, attrs, true);
            this.add(sr);

            // dn: ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalUnit");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("people");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("All users which can be used for testing RHQ user authentication and authorizations.");
            attrs.put(attr);

            sr = new SearchResult("people", null, null, attrs, true);
            this.add(sr);

            // dn: cn=RHQ Admin Group,ou=groups,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("groupOfNames");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("RHQ administrators group");
            attrs.put(attr);

            attr = new BasicAttribute("member");
            attr.add("cn=Robert Smith,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Cannon\\, Brett,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Charles H\\\\Samlin,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Craig \\#1 Sellers,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Beverly \\+1 Balanger,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Bethany \\<Stuart\\> Wallace,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Zachory S\\; Balanger,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Allen \\\"The Hammer\\\" Callen,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Sam Not \\= Smitherson,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=\\ Billy The Kiddough\\ ,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=System/Integration API,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Lee -Fast- Croutche,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Samantha *Won't Quit* Jeopardy,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Brad (The Eagle) Strafford,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Michal M\u011Bchura,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            sr = new SearchResult("RHQ Admin Group", null, null, attrs, true);
            this.add(sr);

            // dn: cn=JBoss Admin Group,ou=groups,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("groupOfNames");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("Group that administers and manages JBoss servers and applications");
            attrs.put(attr);

            attr = new BasicAttribute("member");
            attr.add("cn=John Smith,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Dr. Greg Hause\\, MD,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Cindy\\\\Cynthia Groober,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Biff \\# Rogers,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Steven \\+2 Reed,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Lisa \\<The Great\\> Toller,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Homer J Simpsonite\\; III,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Jessica \\\"Crouching Tiger\\\" Mathers,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Hope \\= Rein,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=\\ Sue Ferguson\\ ,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Phil/Susan Carlson,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Sally -Ainte- Mathers,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Lee *Fast* Croutche,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Kimberly (Six Toe) Krawford,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=We\u00F1dy Sequerl,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            sr = new SearchResult("JBoss Admin Group", null, null, attrs, true);
            this.add(sr);

            // dn: cn=JBoss Monitor Group,ou=groups,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("groupOfNames");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("Group that monitors JBoss servers and applications");
            attrs.put(attr);

            attr = new BasicAttribute("member");
            attr.add("cn=Sheri Smith,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Walsh\\, Brad,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Jim\\\\James Kirk,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Sandra \\# Phillips,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=William Tell Overture \\+1,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Craig \\<Bison\\> Allen,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Walter T Fredrick\\; The Second,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Stanley \\\"Short\\\" Mein,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Noah \\= Sadler,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=\\ Stuart Smiley\\ ,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=System/Integration API 2,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Samantha -Won't Quit- Jeopardy,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Sally *Ainte* Mathers,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Jeff (Top Hat) Wilbright,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attr.add("cn=Phillip Br\u00E3dy,ou=users,dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            sr = new SearchResult("JBoss Monitor Group", null, null, attrs, true);
            this.add(sr);

            // dn "cn=Robert Smith,ou=users,dc=test,dc=rhq,dc=redhat,dc=com"
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("rjosmith");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Smith");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("robert.smith@rhq.redhat.com");
            attr.add("bob.smith@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-111-2222");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Robert Smith");
            attr.add("Robert J Smith");
            attr.add("Bob Smith");
            attrs.put(attr);

            attr = new BasicAttribute("telephonenumber");
            attr.add("555-555-1212");
            attr.add("212");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("swell guy");
            attr.add("Simple user in the RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("carlicense");
            attr.add("HISCAR 123");
            attrs.put(attr);

            sr = new SearchResult("cn=Robert Smith,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=John Smith,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("John Smith");
            attr.add("John J Smith");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Smith");
            attrs.put(attr);

            attr = new BasicAttribute("carlicense");
            attr.add("HISCAR 124");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-111-2223");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("j.smith@rhq.redhat.com");
            attr.add("jsmith@rhq.redhat.com");
            attr.add("john.smith@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("jsmith");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("Simple user in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=John Smith,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Sheri Smith,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Sheri Smith");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Smith");
            attrs.put(attr);

            attr = new BasicAttribute("carlicense");
            attr.add("HERCAR 125");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-111-2225");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("s.smith@rhq.redhat.com");
            attr.add("ssmith@rhq.redhat.com");
            attr.add("sheri.smith@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("ssmith");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("Simple user in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Sheri Smith,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Cannon\, Brett,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Cannon, Brett");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Cannon");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1212");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("bcannon@rhq.redhat.com");
            attr.add("brett.cannon@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("bcannon");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with comma (,) in 'cn' in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Cannon\\, Brett,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Dr. Greg Hause\, MD,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Dr. Greg Hause, MD");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Hause");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-2155");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("ghause@rhq.redhat.com");
            attr.add("dr.feel.good@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("ghause");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with comma (,) in 'cn' in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Dr. Greg Hause\\, MD,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Walsh\, Brad,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Walsh, Brad");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Walsh");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1215");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("bwalsh@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("bwalsh");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with comma (,) in 'cn' in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Walsh\\, Brad,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Charles H\\Samlin,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Charles H\\Samlin");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("H\\Samlin");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1213");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("csamlin@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("csamlin");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with backslash (\\) in 'cn' in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Charles H\\\\Samlin,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Cindy\\Cynthia Groober,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Cindy\\Cynthia Groober");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Groober");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1214");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("cgroober@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("cgroober");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with backslash (\\) in 'cn' in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Cindy\\\\Cynthia Groober,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Jim\\James Kirk,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Jim\\James Kirk");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Kirk");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1215");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("jkirk@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("jkirk");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with backslash (\\) in 'cn' in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Jim\\\\James Kirk,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Craig \#1 Sellers,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Craig #1 Sellers");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Sellers");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1216");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("csellers@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("csellers");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with hash-sign (#) in 'cn' in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Craig \\#1 Sellers,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Biff \# Rogers,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Biff # Rogers");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Rogers");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1217");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("brogers@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("brogers");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with hash-sign (#) in 'cn' in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Biff \\# Rogers,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Sandra \# Phillips,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Sandra \\# Phillips");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Phillips");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1218");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("sphillips@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("sphillips");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with hash-sign (#) in 'cn' in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Sandra \\# Phillips,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Beverly \+1 Balanger,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Beverly +1 Balanger");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Balanger");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1219");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("bbalanger@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("bbalanger");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with plus-sign (+) in 'cn' in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Beverly \\+1 Balanger,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Steven \+2 Reed,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Steven +2 Reed");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Reed");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1220");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("sreed@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("sreed");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with plus-sign (+) in 'cn' in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Steven \\+2 Reed,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=William Tell Overture \+1,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("William Tell Overture +1");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Overture");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1221");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("woverture@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("woverture");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with plus-sign (+) in 'cn' in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=William Tell Overture \\+1,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Bethany \<Stuart\> Wallace,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Bethany <Stuart> Wallace");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Wallace");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1222");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("bwallace@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("bwallace");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with less-than and greater-than sign (<>) in 'cn' in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Bethany \\<Stuart\\> Wallace,ou=users", "javax.naming.directory.DirContext",
                null, attrs, true);
            this.add(sr);

            // dn: cn=Lisa \<The Great\> Toller,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Lisa <The Great> Toller");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Toller");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1223");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("ltoller@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("ltoller");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with less-than and greater-than sign (<>) in 'cn' in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Lisa \\<The Great\\> Toller,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Craig \<Bison\> Allen,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Craig <Bison> Allen");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Allen");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1224");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("callen@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("callen");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with less-than and greater-than sign (<>) in 'cn' in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Craig \\<Bison\\> Allen,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Zachory S\; Balanger,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Zachory S; Balanger");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Balanger");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1225");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("zbalanger@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("zbalanger");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with semi-colon (;) in 'cn' in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Zachory S\\; Balanger,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Homer J Simpsonite\; III,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Homer J Simpsonite; III");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Simpsonite");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1226");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("hsimpsonite@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("hsimpsonite");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with semi-colon (;) in 'cn' in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Homer J Simpsonite\\; III,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Walter T Fredrick\; The Second,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Walter T Fredrick; The Second");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Fredrick");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1227");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("wfredrick@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("wfredrick");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with semi-colon (;) in 'cn' in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Walter T Fredrick\\; The Second,ou=users", "javax.naming.directory.DirContext",
                null, attrs, true);
            this.add(sr);

            // dn: cn=Allen \"The Hammer\" Callen,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Allen \"The Hammer\" Callen");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Callen");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1228");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("acallen@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("acallen");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with quote (\") in 'cn' in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Allen \\\"The Hammer\\\" Callen,ou=users", "javax.naming.directory.DirContext",
                null, attrs, true);
            this.add(sr);

            // dn: cn=Jessica \"Crouching Tiger\" Mathers,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Jessica \"Crouching Tiger\" Mathers");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Simpsonite");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1229");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("jmathers@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("jmathers");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with quote (\") in 'cn' in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Jessica \\\"Crouching Tiger\\\" Mathers,ou=users",
                "javax.naming.directory.DirContext", null, attrs, true);
            this.add(sr);

            // dn: cn=Stanley \"Short\" Mein,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Stanley \"Short\" Mein");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Mein");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1230");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("smein@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("smein");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with quote (\") in 'cn' in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Stanley \\\"Short\\\" Mein,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Sam Not \= Smitherson,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Sam Not = Smitherson");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Smitherson");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1231");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("ssmitherson@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("ssmitherson");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with equal-sign (=) in 'cn' in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Sam Not \\= Smitherson,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Hope \= Rein,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Hope = Rein");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Rein");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1232");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("hrein@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("hrein");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with equal-sign (=) in 'cn' in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Hope \\= Rein,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Noah \= Sadler,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Noah = Sadler");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Sadler");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1233");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("nsadler@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("nsadler");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with equal-sign (=) in 'cn' in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Noah \\= Sadler,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=\ Billy The Kiddough\ ,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add(" Billy The Kiddough ");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Kiddough");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1234");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("bkiddough@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("bkiddough");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with leading and trailing space ( ) in 'cn' in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=\\ Billy The Kiddough\\ ,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=\ Sue Ferguson\ ,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add(" Sue Ferguson ");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Ferguson");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1235");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("sferguson@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("sferguson");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with leading and trailing space ( ) in 'cn' in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=\\ Sue Ferguson\\ ,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=\ Stuart Smiley\ ,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add(" Stuart Smiley ");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Smiley");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1236");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("ssmiley@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("ssmiley");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with leading and trailing space ( ) in 'cn' in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=\\ Stuart Smiley\\ ,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=System/Integration API,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("System/Integration API");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("API");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("sysapi@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("sysapi");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with slash (/) in 'cn' in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=System/Integration API,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Phil/Susan Carlson,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Phil/Susan Carlson");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Carlson");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1238");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("pscarlson@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("pscarlson");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with slash (/) in 'cn' in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Phil/Susan Carlson,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=System/Integration API 2,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("System/Integration API 2");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("API");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("sysapi2@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("sysapi2");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with slash (/) in 'cn' in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=System/Integration API 2,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Lee -Fast- Croutche,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Lee -Fast- Croutche");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Croutche");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1243");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("lecroutche@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("lecroutche");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with hyphen (-) in 'cn', not to be confused with user with similar name in JBoss Admin Group, in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Lee -Fast- Croutche,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Sally -Ainte- Mathers,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Sally -Ainte- Mathers");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Mathers");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1244");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("samathers@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("samathers");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with hyphen (-) in 'cn', not to be confused with user with similar name in JBoss Monitor Group, in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Sally -Ainte- Mathers,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Samantha -Won't Quit- Jeopardy,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Samantha -Won't Quit- Jeopardy");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Jeopardy");
            attrs.put(attr);

            attr = new BasicAttribute("gn");
            attr.add("Samantha");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1245");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("sajeopardy@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("sajeopardy");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with hyphen (-) in 'cn', not to be confused with user with similar name in RHQ Admin Group, in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Samantha -Won't Quit- Jeopardy,ou=users", "javax.naming.directory.DirContext",
                null, attrs, true);
            this.add(sr);

            // dn: cn=Samantha *Won't Quit* Jeopardy,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Samantha *Won't Quit* Jeopardy");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Jeopardy");
            attrs.put(attr);

            attr = new BasicAttribute("gn");
            attr.add("Samantha");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1246");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("sjeopardy@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("sjeopardy");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with asterisk (*) in 'cn', not to be confused with user with similar name in JBoss Monitor Group, in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Samantha *Won't Quit* Jeopardy,ou=users", "javax.naming.directory.DirContext",
                null, attrs, true);
            this.add(sr);

            // dn: cn=Lee *Fast* Croutche,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Lee *Fast* Croutche");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Croutche");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1247");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("lcroutche@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("lcroutche");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with asterisk (*) in 'cn', not to be confused with user with similar name in RHQ Admin Group, in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Lee *Fast* Croutche,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Sally *Ainte* Mathers,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Sally *Ainte* Mathers");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Mathers");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1248");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("smathers@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("smathers");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with asterisk (*) in 'cn', not to be confused with user with similar name in JBoss Admin Group, in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Sally *Ainte* Mathers,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Brad (The Eagle) Strafford,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Brad (The Eagle) Strafford");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Strafford");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1249");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("bstrafford@rhq.redhat.com");
            attr.add("the.eagle@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("bstrafford");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with parenthesis () in 'cn' in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Brad (The Eagle) Strafford,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Kimberly (Six Toe) Krawford,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Kimberly (Six Toe) Krawford");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Krawford");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1250");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("kkrawford@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("kkrawford");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with parenthesis () in 'cn' in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Kimberly (Six Toe) Krawford,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Jeff (Top Hat) Wilbright,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Jeff (Top Hat) Wilbright");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Wilbright");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-555-1251");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("jwilbright@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("jwilbright");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with parenthesis () in 'cn' in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Jeff (Top Hat) Wilbright,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Michal MÄ›chura,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Michal MÄ›chura");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("M\u011Bchura");
            attrs.put(attr);

            attr = new BasicAttribute("gn");
            attr.add("Michal");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-777-1212");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("mmechura@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("mmechura");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("RHQ Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with non-ASCII character (\u011B) in 'cn' in the RHQ Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Michal M\u011Bchura,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=WeÃ±dy Sequerl,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("We\u00F1dy Sequerl");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Sequerl");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-777-1213");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("wsequerl@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("wsequerl");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Admin Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with non-ASCII character (\u00F1) in 'cn' in the JBoss Admin Group");
            attrs.put(attr);

            sr = new SearchResult("cn=We\u00F1dy Sequerl,ou=users", null, null, attrs, true);
            this.add(sr);

            // dn: cn=Phillip BrÃ£dy,ou=users,dc=test,dc=rhq,dc=redhat,dc=com
            attrs = new BasicAttributes();

            attr = new BasicAttribute("baseName");
            attr.add("dc=test,dc=rhq,dc=redhat,dc=com");
            attrs.put(attr);

            attr = new BasicAttribute("objectClass");
            attr.add("organizationalPerson");
            attr.add("person");
            attr.add("inetOrgPerson");
            attr.add("top");
            attrs.put(attr);

            attr = new BasicAttribute("cn");
            attr.add("Phillip Br\u00E3dy");
            attrs.put(attr);

            attr = new BasicAttribute("sn");
            attr.add("Br\u00E3dy");
            attrs.put(attr);

            attr = new BasicAttribute("homephone");
            attr.add("555-777-1214");
            attrs.put(attr);

            attr = new BasicAttribute("mail");
            attr.add("pbrady@rhq.redhat.com");
            attrs.put(attr);

            attr = new BasicAttribute("uid");
            attr.add("pbrady");
            attrs.put(attr);

            attr = new BasicAttribute("userpassword");
            attr.add("cmVkaGF0");
            attrs.put(attr);

            attr = new BasicAttribute("ou");
            attr.add("JBoss Monitor Group");
            attrs.put(attr);

            attr = new BasicAttribute("description");
            attr.add("User with non-ASCII character (\u00E3) in 'cn' in the JBoss Monitor Group");
            attrs.put(attr);

            sr = new SearchResult("cn=Phillip Br\u00E3dy,ou=users", null, null, attrs, true);
            this.add(sr);

        }

        /**
         * Unescapes an LDAP search filter value. This is done by converting 
         * each occurance of \xx (where xx is a hex value) to its character 
         * reprentation. If xx is not a valid hex value, the \ is stripped from
         * the result.
         * 
         * @param value a value from an LDAP search filter
         * @return unescaped value
         * @throws Exception 
         */
        private String unescapeFilterValue(String value) {
            boolean strip = false;
            int i = 0, len = value.length();
            char c;
            StringBuffer sb = new StringBuffer(len);
            while (i < len) {
                c = value.charAt(i++);
                if (c == '\\') {
                    if (i + 2 < len) {
                        try {
                            c = (char) Integer.parseInt(value.substring(i, i + 2), 16);
                        } catch (NumberFormatException nfe) {
                            strip = true;
                        }
                        if (!strip)
                            i += 2;
                    }
                } // fall through: \ escapes itself, quotes any character but u
                if (!strip)
                    sb.append(c);
                strip = false;
            }
            return sb.toString();
        }

        /**
         * Query the test data for an LDAP entry using a simple query. The 
         * simple query is defined by a {@link Hashtable} which represents the 
         * required attributes and their values. Each LDAP entry that satisifies 
         * all the attributes and value contained in <code>attrSet</code> are 
         * returned.
         * 
         * @param attrSet The attributes and values that an LDAP entry must have 
         *      to satisfy the query
         * @return A {@link List} of 0 or more {@link SearchResult} LDAP entries 
         *      which satisified the query
         * @throws NamingException 
         */
        public List<SearchResult> getSearchResults(Hashtable<String, String> attrSet) throws NamingException {
            List<SearchResult> list = new ArrayList<SearchResult>();
            int matchCount, keyCount;
            boolean match;

            for (SearchResult sr : this) {
                matchCount = 0;
                keyCount = 0;
                Attributes attrs = sr.getAttributes();
                Enumeration<String> keye = attrSet.keys();
                while (keye.hasMoreElements()) {
                    match = false;
                    String searchAttrName = (String) keye.nextElement();
                    Attribute attrib = attrs.get(searchAttrName);
                    if (attrib != null) {
                        for (Enumeration<?> vals = attrib.getAll(); vals.hasMoreElements();) {
                            String value = (String) vals.nextElement();
                            String utf8Value = new String(unescapeFilterValue(LDAPStringUtil.encodeForFilter(value)));
                            match = utf8Value.equalsIgnoreCase(unescapeFilterValue(attrSet.get(searchAttrName)));
                            if (match) {
                                break;
                            }
                        }
                    }
                    if (match)
                        matchCount++;
                    keyCount++;
                }
                if (matchCount > 0 && matchCount == keyCount) {
                    list.add(sr);
                }
            }

            return list;
        }
    }

}
