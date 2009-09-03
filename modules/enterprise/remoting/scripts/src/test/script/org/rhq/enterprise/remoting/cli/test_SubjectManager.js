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


executeAllTests();


function testFindWithFiltering() {
   rhq.login('rhqadmin', 'rhqadmin');

   var subject = SubjectManager.getSubjectByName('rhqadmin');

    var criteria = SubjectCriteria();
    criteria.addFilterId(subject.id);
    criteria.addFilterName(subject.name);
    criteria.addFilterFirstName(subject.firstName);
    criteria.addFilterLastName(subject.lastName);
    criteria.addFilterEmailAddress(subject.emailAddress);
    criteria.addFilterSmsAddress(subject.smsAddress);
    criteria.addFilterPhoneNumber(subject.phoneNumber);
    criteria.addFilterDepartment(subject.department);
    criteria.addFilterFactive(subject.factive);

    var subjects = SubjectManager.findSubjectsByCriteria(criteria);

    Assert.assertNumberEqualsJS(subjects.size(), 1, 'Failed to find subjects when filtering');
    
    rhq.logout();    
}

function testFindWithFetchingAssociations() {
    rhq.login('rhqadmin', 'rhqadmin');
   
    var criteria = SubjectCriteria();
    criteria.addFilterName('rhqadmin');
    criteria.fetchConfiguration(true);
    criteria.fetchRoles(true);
    criteria.fetchSubjectNotifications(true);

    var subjects = SubjectManager.findSubjectsByCriteria(criteria);

    Assert.assertNumberEqualsJS(subjects.size(), 1, 'Failed to find subject when fetching associations');
    
    rhq.logout();    
}

function testFindWithSorting() {
    rhq.login('rhqadmin', 'rhqadmin');
   
    var criteria = SubjectCriteria();
    criteria.addSortFirstName(PageOrdering.ASC);
    criteria.addSortLastName(PageOrdering.DESC);
    criteria.addSortEmailAddress(PageOrdering.ASC);
    criteria.addSortSmsAddress(PageOrdering.DESC);
    criteria.addSortPhoneNumber(PageOrdering.ASC);
    criteria.addSortDepartment(PageOrdering.DESC);

    var subjects = SubjectManager.findSubjectsByCriteria(criteria);

    Assert.assertTrue(subjects.size() > 0, 'Failed to find subjects when sorting');

    // TODO verify sort order
    
    rhq.logout();
}

function testLoginLogout() {
   rhq.login('rhqadmin', 'rhqadmin');
   
   Assert.assertNotNull( subject, "Should have returned a subject" );
   Assert.assertEquals( subject.getName(), "rhqadmin", "Unexpected Subject name");
   var sessionId = subject.getSessionId();

   // should return same sessionId
   rhq.login('rhqadmin', 'rhqadmin');   
   Assert.assertNotNull( subject, "Should have returned a subject" );
   Assert.assertEquals( subject.getName(), "rhqadmin", "Unexpected Subject name");
   Assert.assertEquals( subject.getSessionId(), sessionId, "Unexpected Subject session");   
   
   rhq.logout();
   // Assert.assertNull( subject, "Should be no active subject" );
   
   // should return new sessionId   
   rhq.login('rhqadmin', 'rhqadmin');   
   Assert.assertNotNull( subject, "Should have returned a subject" );
   Assert.assertEquals( subject.getName(), "rhqadmin", "Unexpected Subject name");
   Assert.assertTrue( (subject.getSessionId() != sessionId), "Unexpected Subject session");

   rhq.logout();

   print( 'FOO!' )
}