/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.auth.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ejb.EJBException;
import javax.persistence.EntityManager;
import javax.security.auth.login.LoginException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.auth.SessionNotFoundException;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.search.SavedSearchManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.SessionTestHelper;
import org.testng.annotations.Test;

/**
 * Tests the subject manager.
 */
@Test
public class SubjectManagerBeanTest extends AbstractEJB3Test {

    private static final Log LOG = LogFactory.getLog(SubjectManagerBeanTest.class);

    private static final String RHQADMIN = "rhqadmin";

    private static final String ITEST_USER = "smb_itest_user";

    private SubjectManagerLocal subjectManager;
    private AuthorizationManagerLocal authorizationManager;
    private RoleManagerLocal roleManager;
    private SavedSearchManagerLocal savedSearchManager;

    @Override
    protected void beforeMethod() {
        subjectManager = LookupUtil.getSubjectManager();
        authorizationManager = LookupUtil.getAuthorizationManager();
        roleManager = LookupUtil.getRoleManager();
        savedSearchManager = LookupUtil.getSavedSearchManager();
        createITestSubject();
    }

    private Subject createITestSubject() {
        Subject subjectToCreate = new Subject(ITEST_USER, true, false);
        return subjectManager.createSubject(subjectManager.getOverlord(), subjectToCreate, ITEST_USER);
    }

    /**
     * This runs after each test method. It clears out all sessions.
     */
    @Override
    protected void afterMethod() {
        deleteITestSubject();

        // create a list of all users we know our tests have used
        List<String> usernames = new ArrayList<String>();
        usernames.add("admin");
        usernames.add(RHQADMIN);
        usernames.add("new_user");
        usernames.add(ITEST_USER);

        SessionManager session_manager = SessionManager.getInstance();

        // clean out all users' sessions (a user can have more than one session)
        while (usernames.size() > 0) {
            try {
                String doomed_user = usernames.get(0);
                session_manager.invalidate(doomed_user);
            } finally {
                usernames.remove(0);
            }
        }
    }

    private void deleteITestSubject() {
        Subject subject = subjectManager.getSubjectByName(ITEST_USER);
        if (subject != null) {
            subjectManager.deleteSubjects(subjectManager.getOverlord(), new int[] { subject.getId() });
        }
    }

    /**
     * Tests persisting and retrieving user configuration.
     *
     * @throws Exception
     */
    public void testUserConfiguration() throws Exception {
        Subject superuser;

        getTransactionManager().begin();

        try {
            superuser = subjectManager.getOverlord();

            // create the user
            Subject new_user = new Subject("dummy-user", true, false);
            new_user = subjectManager.createSubject(superuser, new_user);
            assert new_user.getUserConfiguration() == null : "There should not be any configuration yet";

            // set and persist an empty configuration
            Configuration config = new Configuration();
            new_user.setUserConfiguration(config);
            assert new_user.getUserConfiguration() != null : "An empty configuration should have been set";
            assert new_user.getUserConfiguration().getProperties().size() == 0 : "An empty config should have been set";
            assert new_user.getUserConfiguration().getId() == 0 : "Configuration wasn't persisted - should not have an ID";

            new_user = subjectManager.updateSubject(superuser, new_user); // let superuser change it
            config = new_user.getUserConfiguration();
            assert config != null : "An empty configuration should have been persisted";
            assert config.getProperties().size() == 0 : "An empty config should have been persisted";
            assert config.getId() != 0 : "Configuration was persisted - should have an ID";

            // set and persist a non-empty configuration
            config.put(new PropertySimple("firstname", "firstvalue"));
            config.put(new PropertySimple("secondname", "secondvalue"));
            assert new_user.getUserConfiguration() != null : "A full configuration should have been set";
            assert new_user.getUserConfiguration().getProperties().size() == 2 : "A full config should have been set";

            new_user = createSession(new_user);
            new_user = subjectManager.updateSubject(new_user, new_user); // let the user itself change it
            config = new_user.getUserConfiguration();
            assert config != null : "A full configuration should have been persisted";
            assert config.getProperties().size() == 2 : "A full config should have been persisted";
            assert config.getId() != 0 : "Configuration was persisted - should have an ID";
            assert config.getSimple("firstname").getStringValue().equals("firstvalue") : "Configuration wasn't persisted properly";
            assert config.getSimple("secondname").getStringValue().equals("secondvalue") : "Configuration wasn't persisted properly!";

            // let's try the load method to make sure it can eagerly load the configuration
            Subject loaded = subjectManager.loadUserConfiguration(new_user.getId());
            config = loaded.getUserConfiguration();
            assert config != null : "A full configuration should have been persisted";
            assert config.getProperties().size() == 2 : "A full config should have been persisted";
            assert config.getId() != 0 : "Configuration was persisted - should have an ID";
            assert config.getSimple("firstname").getStringValue().equals("firstvalue") : "Configuration wasn't persisted properly";
            assert config.getSimple("secondname").getStringValue().equals("secondvalue") : "Configuration wasn't persisted properly!";

            // set and persist a null configuration
            new_user.setUserConfiguration(null);
            assert new_user.getUserConfiguration() == null : "There should not be any configuration anymore";

            new_user = subjectManager.updateSubject(superuser, new_user); // let superuser change it
            assert new_user.getUserConfiguration() == null : "An empty configuration should have been persisted";
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Makes sure you cannot disable super users.
     *
     * @throws Exception
     */
    public void testTryToDisableSuperUsers() throws Exception {
        Subject superuser;
        Subject rhqadmin;

        getTransactionManager().begin();
        try {
            superuser = subjectManager.getOverlord();
            rhqadmin = subjectManager.getSubjectByName(RHQADMIN);
            rhqadmin = createSession(rhqadmin);

            try {
                superuser.setFactive(false);
                subjectManager.updateSubject(rhqadmin, superuser);
                assert false : "You should not be able to disable the superuser";
            } catch (PermissionException se) {
                superuser.setFactive(true);
            }
        } finally {
            getTransactionManager().rollback();
        }

        getTransactionManager().begin();
        try {
            try {
                rhqadmin.setFactive(false);
                subjectManager.updateSubject(superuser, rhqadmin);
                assert false : "You should not be able to delete the rhqadmin user";
            } catch (PermissionException se) {
                rhqadmin.setFactive(true);
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Makes sure you cannot delete the super users.
     *
     * @throws Exception
     */
    public void testTryToDeleteSuperUsers() throws Exception {
        Subject superuser;
        Subject rhqadmin;

        getTransactionManager().begin();
        try {
            superuser = subjectManager.getOverlord();
            superuser = createSession(superuser);
            rhqadmin = subjectManager.getSubjectByName(RHQADMIN);
            rhqadmin = createSession(rhqadmin);

            try {
                subjectManager.deleteUsers(superuser, new int[] { rhqadmin.getId() });
                assert false : "You should not be able to delete the rhqadmin user";
            } catch (PermissionException se) {
            }
        } finally {
            getTransactionManager().rollback();
        }

        getTransactionManager().begin();
        try {
            try {
                subjectManager.deleteUsers(rhqadmin, new int[] { superuser.getId() });
                assert false : "You should not be able to delete the superuser";
            } catch (PermissionException se) {
            }
        } finally {
            getTransactionManager().rollback();
        }

        // but you should be allowed to change the rhqadmin's principal password
        getTransactionManager().begin();
        try {
            subjectManager.changePassword(rhqadmin, rhqadmin.getName(), "change-me");
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Tests CRUD on subjects.
     *
     * @throws Exception
     */
    public void testSubjects() throws Exception {
        Subject new_user = null;

        getTransactionManager().begin();

        List<Permission> all_global_perms = new ArrayList<Permission>();
        all_global_perms.add(Permission.MANAGE_SECURITY);
        all_global_perms.add(Permission.MANAGE_INVENTORY);
        all_global_perms.add(Permission.MANAGE_SETTINGS);

        // get the system super subject
        Subject superuser = subjectManager.getSubjectById(1);
        assert superuser.getId() == 1;
        assert superuser.getName().equals("admin");
        assert authorizationManager.getExplicitGlobalPermissions(superuser).containsAll(all_global_perms);

        // get the rhqadmin subject
        Subject rhqadmin = subjectManager.getSubjectByName(RHQADMIN);
        assert rhqadmin.getId() == 2;
        assert rhqadmin.getName().equals(RHQADMIN);
        assert authorizationManager.getExplicitGlobalPermissions(rhqadmin).containsAll(all_global_perms);

        rhqadmin = createSession(rhqadmin); // our test needs to ensure the rhqadmin user has a session

        // check the subjects that do and do not have principals
        Collection<String> all_users_with_principals = subjectManager.findAllUsersWithPrincipals();
        assert !all_users_with_principals.contains(superuser.getName());
        assert all_users_with_principals.contains(rhqadmin.getName());
        assert !subjectManager.isUserWithPrincipal(superuser.getName());
        assert subjectManager.isUserWithPrincipal(rhqadmin.getName());

        // get all subjects, whether or not they have a principal
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject subject = null;
        try {
            subject = subjectManager.loginUnauthenticated(RHQADMIN);
        } catch (Exception e) {
            assert false : "There must be at least rhqadmin user";
        }

        SubjectCriteria c = new SubjectCriteria();
        c.addFilterFsystem(false);
        c.addSortName(PageOrdering.ASC);
        PageList<Subject> all_subjects = subjectManager.findSubjectsByCriteria(subject, c);

        assert all_subjects.size() >= 1 : "There must be at least rhqadmin user";
        assert !all_subjects.contains(superuser) : "The superuser should not have been returned in the list";
        assert all_subjects.contains(rhqadmin) : "Missing user [" + rhqadmin + "] from: " + all_subjects;

        // create, modify then delete the user
        new_user = new Subject();
        new_user.setFsystem(false);
        new_user.setFactive(true);
        new_user.setName("dummy-user");
        new_user.setFirstName("my-firstname");
        new_user.setLastName("my-lastname");
        new_user.setEmailAddress("email@address.com");
        new_user.setPhoneNumber("1-800-555-1212");
        new_user.setSmsAddress("sms address");
        new_user.setDepartment("my-department");

        new_user = subjectManager.createSubject(rhqadmin, new_user);
        new_user = createSession(new_user);
        assert !subjectManager.isUserWithPrincipal(new_user.getName());
        subjectManager.createPrincipal(subjectManager.getOverlord(), new_user.getName(), "my-password");
        assert subjectManager.isUserWithPrincipal(new_user.getName());

        // make sure it was persisted and you can actually login with it
        assert new_user.getId() != 0;
        Subject login_new_user = subjectManager.loginUnauthenticated(new_user.getName());
        assert login_new_user.equals(new_user);
        new_user = login_new_user; // login_new_user was given a new session ID

        subjectManager.changePassword(new_user, new_user.getName(), "my-new-password");
        subjectManager.changePassword(rhqadmin, new_user.getName(), "my-new-password"); // see that rhqadmin can change it too

        getTransactionManager().commit();
        getTransactionManager().begin();

        try {
            subjectManager.changePassword(new_user, rhqadmin.getName(), "not-allowed");
            assert false : "The new user does not have permission to change another's password";
        } catch (PermissionException se) {
        } finally {
            // PermissionException causes a rollback, let's start a new tx
            getTransactionManager().rollback();
        }

        getTransactionManager().begin();

        // this new user has no roles - therefore, no global permissions
        assert authorizationManager.getExplicitGlobalPermissions(new_user).size() == 0;

        try {
            subjectManager.deleteUsers(new_user, new int[] { new_user.getId() });
            assert false : "The new user should not have had the permission to delete itself";
        } catch (PermissionException e) {
        } finally {
            // PermissionException causes a rollback, let's start a new tx
            getTransactionManager().rollback();
        }

        getTransactionManager().begin();

        // delete the new user
        assert subjectManager.getSubjectByName(new_user.getName()).equals(new_user);
        assert subjectManager.isUserWithPrincipal(new_user.getName());
        subjectManager.deleteUsers(rhqadmin, new int[] { new_user.getId() });
        assert subjectManager.getSubjectByName(new_user.getName()) == null;
        assert !subjectManager.isUserWithPrincipal(new_user.getName());

        getTransactionManager().commit();
    }

    /** 
     * Tests finding Subjects with Roles.
     * @throws SystemException 
     * @throws NotSupportedException 
     */
    public void testFindSubjectsWithRoles() throws NotSupportedException, SystemException {
        getTransactionManager().begin();
        EntityManager entityMgr = getEntityManager();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        RoleManagerLocal roleManager = LookupUtil.getRoleManager();
        Subject overlord = subjectManager.getOverlord();

        try {
            //create new subject
            Subject subject = SessionTestHelper.createNewSubject(entityMgr, "testSubject");
            //create new role for subject
            Role roleWithSubject = SessionTestHelper.createNewRoleForSubject(entityMgr, subject, "role with subject");
            roleWithSubject.addPermission(Permission.VIEW_RESOURCE);
            Role newRole = new Role("role without subject");
            Role roleWithoutSubject = roleManager.createRole(overlord, newRole);

            //exercise findAvailableSubjectsForRole
            Integer[] pendingSubjectIds = new Integer[0];
            PageList<Subject> subjects = subjectManager.findAvailableSubjectsForRole(subjectManager.getOverlord(),
                roleWithoutSubject.getId(), pendingSubjectIds, PageControl.getUnlimitedInstance());
            assert subjects.size() > 0 : "Unable to locate subject(s) available for role with id '"
                + roleWithSubject.getId() + "'.";//Should be at least one.
            //            boolean located = false;
            Subject locatedSubject = null;
            for (Subject s : subjects) {
                if (s.getName().equals(subject.getName())) {
                    locatedSubject = s;
                }
            }
            assert locatedSubject != null : "Unable to located subject with name '" + subject.getName() + "'.";
            assert locatedSubject.getId() == subject.getId() : "Subject id does not match expected subject identifier '"
                + subject.getId() + "'";//should match.

        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Tests getting a super user subject.
     *
     * @throws Exception
     */
    public void testGetSuperUser() throws Exception {
        getTransactionManager().begin();

        try {
            Subject superuser1 = subjectManager.getOverlord();
            assert superuser1.getId() == 1;

            Subject superuser2 = subjectManager.getOverlord();
            assert superuser2.getId() == 1;

            assert superuser1.equals(superuser2);

            // that second call should have used the same session since not enough time elapsed to expire the session
            assert superuser1.getSessionId().equals(superuser2.getSessionId());
        } finally {
            getTransactionManager().rollback();
        }
    }

    /**
     * Tests logging in and out when not requiring passwords.
     *
     * @throws Exception
     */
    public void testLoginUnauthenticated() throws Exception {
        getTransactionManager().begin();

        try {
            Subject subject1 = subjectManager.loginUnauthenticated(RHQADMIN);
            int session1 = subject1.getSessionId();

            Thread.sleep(500); // just wait a bit

            Subject subject2 = subjectManager.loginUnauthenticated(RHQADMIN);
            int session2 = subject2.getSessionId();

            assert session1 != session2 : "The same sessionId should never be assigned when logging in twice";
            assert subject1.equals(subject2);

            Subject s = subjectManager.getSubjectByNameAndSessionId(RHQADMIN, subject1.getSessionId());
            assert s.getSessionId() == session1;
            s = subjectManager.getSubjectByNameAndSessionId(RHQADMIN, subject2.getSessionId());
            assert s.getSessionId() == session2;

            subjectManager.logout(session1);
            try {
                s = subjectManager.getSubjectByNameAndSessionId(RHQADMIN, subject1.getSessionId());
                assert false : "Session should be invalid";
            } catch (SessionNotFoundException ok) {
            }

            s = subjectManager.getSubjectByNameAndSessionId(RHQADMIN, subject2.getSessionId());
            assert s.getSessionId() == session2;

            // this should ne a no-op, no exception
            subjectManager.logout(session1);

            subjectManager.logout(session2);
            try {
                s = subjectManager.getSubjectByNameAndSessionId(RHQADMIN, subject2.getSessionId());
                fail("Session should be invalid");
            } catch (SessionNotFoundException e) {
                // expected
            }

            try {
                subjectManager.loginUnauthenticated("rhqadminX");
                assert false : "Should not have logged in - provided a bad username";
            } catch (LoginException ok) {
            }
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testDeleteUser() throws Exception {
        getTransactionManager().begin();

        try {
            Subject overlord = subjectManager.getOverlord();

            Subject new_user = new Subject("dummy-user", true, false);
            new_user = subjectManager.createSubject(overlord, new_user);

            Role new_role = new Role("dummy-role");
            RoleManagerLocal roleManager = LookupUtil.getRoleManager();
            new_role = roleManager.createRole(overlord, new_role);

            //            new_user.addRole(new_role);
            int[] subjectIds = new int[] { new_user.getId() };
            roleManager.addSubjectsToRole(overlord, new_role.getId(), subjectIds);
            assert new_role.getSubjects().contains(new_user) : "New_role does not contain new_user";
            int count = new_role.getSubjects().size();

            subjectManager.deleteUsers(overlord, subjectIds);

            assert new_role.getSubjects().size() == count - 1 : "User was not deleted from new_role";
        } finally {
            getTransactionManager().rollback();
        }

    }

    public void testViewUsersPermission_subjectWithViewUsersRoleCanViewOtherUsers() throws Exception {
        getTransactionManager().begin();

        try {
            Subject overlord = subjectManager.getOverlord();
            Subject rhqadmin = subjectManager.getSubjectByName(RHQADMIN);

            Role roleWithViewUsersPerm = new Role("role" + UUID.randomUUID());
            roleWithViewUsersPerm.addPermission(Permission.VIEW_USERS);
            roleWithViewUsersPerm = roleManager.createRole(overlord, roleWithViewUsersPerm);

            Subject subjectWithViewUsersRole = new Subject("subject" + UUID.randomUUID(), true, false);
            subjectWithViewUsersRole.addRole(roleWithViewUsersPerm);
            subjectWithViewUsersRole = subjectManager.createSubject(overlord, subjectWithViewUsersRole, "password");
            subjectWithViewUsersRole = subjectManager.loginUnauthenticated(subjectWithViewUsersRole.getName());

            Subject anotherSubject = new Subject("subject" + UUID.randomUUID(), true, false);
            anotherSubject = subjectManager.createSubject(overlord, anotherSubject, "password");

            PageList<Subject> subjects = subjectManager.findSubjectsByCriteria(subjectWithViewUsersRole,
                new SubjectCriteria());
            Set<Subject> subjectSet = new HashSet<Subject>(subjects);
            assertTrue(subjectSet.contains(subjectWithViewUsersRole));
            assertTrue(subjectSet.contains(anotherSubject));
            assertTrue(subjectSet.contains(rhqadmin));
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testViewUsersPermission_rhqadminCanViewOtherUsers() throws Exception {
        getTransactionManager().begin();

        try {
            Subject overlord = subjectManager.getOverlord();

            Subject rhqadmin = subjectManager.getSubjectByName(RHQADMIN);
            rhqadmin = subjectManager.loginUnauthenticated(rhqadmin.getName());

            Subject anotherSubject = new Subject("subject" + UUID.randomUUID(), true, false);
            anotherSubject = subjectManager.createSubject(overlord, anotherSubject, "password");

            PageList<Subject> subjects = subjectManager.findSubjectsByCriteria(rhqadmin, new SubjectCriteria());
            Set<Subject> subjectSet = new HashSet<Subject>(subjects);
            assertTrue(subjectSet.contains(anotherSubject));
            assertTrue(subjectSet.contains(rhqadmin));
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testViewUsersPermission_subjectWithNonViewUsersRoleCannotViewOtherUsers() throws Exception {
        getTransactionManager().begin();

        try {
            Subject overlord = subjectManager.getOverlord();

            Role roleWithoutViewUsersPerm = new Role("role" + UUID.randomUUID());
            roleWithoutViewUsersPerm = roleManager.createRole(overlord, roleWithoutViewUsersPerm);

            Subject subjectWithNonViewUsersRole = new Subject("subject" + UUID.randomUUID(), true, false);
            subjectWithNonViewUsersRole.addRole(roleWithoutViewUsersPerm);
            subjectWithNonViewUsersRole = subjectManager.createSubject(overlord, subjectWithNonViewUsersRole,
                "password");
            subjectWithNonViewUsersRole = subjectManager.loginUnauthenticated(subjectWithNonViewUsersRole.getName());

            Subject anotherSubject = new Subject("subject" + UUID.randomUUID(), true, false);
            anotherSubject = subjectManager.createSubject(overlord, anotherSubject, "password");

            PageList<Subject> subjects = subjectManager.findSubjectsByCriteria(subjectWithNonViewUsersRole,
                new SubjectCriteria());
            Set<Subject> subjectSet = new HashSet<Subject>(subjects);
            assertEquals(1, subjectSet.size());
            assertTrue(subjectSet.contains(subjectWithNonViewUsersRole));
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void testViewUsersPermission_subjectWithNoRolesCannotViewOtherUsers() throws Exception {
        getTransactionManager().begin();

        try {
            Subject overlord = subjectManager.getOverlord();

            Subject subjectWithNoRoles = new Subject("subject" + UUID.randomUUID(), true, false);
            subjectWithNoRoles = subjectManager.createSubject(overlord, subjectWithNoRoles, "password");
            subjectWithNoRoles = subjectManager.loginUnauthenticated(subjectWithNoRoles.getName());

            Subject anotherSubject = new Subject("subject" + UUID.randomUUID(), true, false);
            anotherSubject = subjectManager.createSubject(overlord, anotherSubject, "password");

            PageList<Subject> subjects = subjectManager.findSubjectsByCriteria(subjectWithNoRoles,
                new SubjectCriteria());
            Set<Subject> subjectSet = new HashSet<Subject>(subjects);
            assertEquals(1, subjectSet.size());
            assertTrue(subjectSet.contains(subjectWithNoRoles));
        } finally {
            getTransactionManager().rollback();
        }
    }
    
    public void testSubjectWithNoRolesCannotLoginIfItIsDisabled() throws Exception {
        getTransactionManager().begin();

        try {
            Subject overlord = subjectManager.getOverlord();
            final String name = "subject" + UUID.randomUUID();
            final String password = "password";
            Subject subjectWithNoRoles = new Subject(name, true, false);
            subjectWithNoRoles = subjectManager.createSubject(overlord, subjectWithNoRoles, password);
            subjectManager.loginLocal(name, password);
            fail("Exception should be thrown when logging in without any roles if it is disabled, but wasn't.");
        } catch (LoginException ex) {
            assertTrue(true);
        } finally {
            getTransactionManager().rollback();
        }
    }

    public void subjectCannotUpdateAnotherSubjectWithoutPermission() throws LoginException {
        executeInTransaction(new TransactionCallback() {

            @Override
            public void execute() throws Exception {
                Subject fakeSubject = new Subject("fakeUser", true, false);
                Subject itestSubject = subjectManager.loginUnauthenticated(ITEST_USER);
                try {
                    subjectManager.updateSubject(itestSubject, fakeSubject, "newPassword");
                    fail("Subject without permission should not be able to update another subject");
                } catch (PermissionException e) {
                    assertTrue(e.getMessage().contains("do not have permission to update user"));
                }
            }
        });
    }

    public void nobodyCanDisableASystemSubject() {
        executeInTransaction(new TransactionCallback() {

            @Override
            public void execute() throws Exception {
                Subject rhqAdminSubject = subjectManager.getSubjectByName(RHQADMIN);
                try {
                    Subject changedSubject = new Subject(rhqAdminSubject.getName(), false, rhqAdminSubject.getFsystem());
                    changedSubject.setId(rhqAdminSubject.getId());
                    subjectManager.updateSubject(subjectManager.getOverlord(), changedSubject, "newPassword");
                    fail("Nobody should be able to disable a system subject");
                } catch (PermissionException e) {
                    assertTrue(e.getMessage().startsWith("You cannot disable the system user"));
                }
            }
        });
    }

    public void nobodyCanChangeASubjectName() {
        executeInTransaction(new TransactionCallback() {

            @Override
            public void execute() throws Exception {
                Subject itestSubject = subjectManager.getSubjectByName(ITEST_USER);
                Subject changedSubject = new Subject("pipo", itestSubject.getFactive(), itestSubject.getFsystem());
                changedSubject.setId(itestSubject.getId());
                try {
                    subjectManager.updateSubject(subjectManager.getOverlord(), changedSubject, "newPassword");
                    fail("Nobody should be able to change a subject name");
                } catch (EJBException e) {
                    Exception cause = e.getCausedByException();
                    assertEquals(IllegalArgumentException.class, cause.getClass());
                    assertTrue(cause.getMessage().equals("You cannot change a user's username."));
                }
            }
        });
    }

    public void nobodyCanChangeAnUnknowSubject() {
        executeInTransaction(new TransactionCallback() {

            @Override
            public void execute() throws Exception {
                try {
                    Subject fakeSubject = new Subject("fakeUser", true, false);
                    subjectManager.updateSubject(subjectManager.getOverlord(), fakeSubject, "newPassword");
                    fail("Nobody should be able to change an unknown subject");
                } catch (EJBException e) {
                    Exception cause = e.getCausedByException();
                    assertEquals(IllegalArgumentException.class, cause.getClass());
                    assertTrue(cause.getMessage().startsWith("No user exists with id"));
                }
            }
        });
    }

    public void subjectCanUpdateItself() {
        executeInTransaction(new TransactionCallback() {

            @Override
            public void execute() throws Exception {
                Subject itestSubject = subjectManager.loginUnauthenticated(ITEST_USER);
                Subject changedSubject = new Subject(itestSubject.getName(), itestSubject.getFactive(),
                    itestSubject.getFsystem());
                changedSubject.setId(itestSubject.getId());
                changedSubject.setEmailAddress("pipo@molo.com");
                try {
                    changedSubject = subjectManager.updateSubject(itestSubject, changedSubject, "newPassword");
                    assertEquals("pipo@molo.com", changedSubject.getEmailAddress());
                } catch (Exception e) {
                    LOG.error(e);
                    fail("Subject should be able to update itself");
                }
            }
        });
    }

    public void subjectWhitoutManageSecurityPermissionCannotUpdateItsRoles() throws LoginException {
        executeInTransaction(new TransactionCallback() {

            @Override
            public void execute() throws Exception {
                Subject itestSubject = subjectManager.loginUnauthenticated(ITEST_USER);
                final PageList<Role> allRoles = roleManager.findRoles(PageControl.getUnlimitedInstance());
                Subject changedSubject = new Subject(itestSubject.getName(), itestSubject.getFactive(),
                    itestSubject.getFsystem());
                changedSubject.setId(itestSubject.getId());
                changedSubject.getRoles().addAll(allRoles);
                try {
                    subjectManager.updateSubject(itestSubject, changedSubject, "newPassword");
                    fail("Subject whitout " + Permission.MANAGE_SECURITY
                        + " permission should not be able to update its roles");
                } catch (PermissionException e) {
                    assertTrue(e.getMessage().contains("is not authorized for"));
                }
            }
        });
    }

    public void shouldBeAbleToDeleteASubjectWhoOwnsASavedSearch() {
        // See https://bugzilla.redhat.com/show_bug.cgi?id=952652
        SavedSearch savedSearch = new SavedSearch(SearchSubsystem.GROUP, "fake saved search", "pipo", subjectManager.getSubjectByName(ITEST_USER));
        Integer savedSearchId = savedSearchManager.createSavedSearch(subjectManager.getOverlord(), savedSearch);
        boolean subjectDeleted = false;
        try {
            deleteITestSubject();
            subjectDeleted = true;
        } catch (Exception e) {
            LOG.error("Failed to delete a subject who owns a saved search", e);
            fail("Failed to delete a subject who owns a saved search");
        } finally {
            // This is only necessary if we failed to delete subject
            if (!subjectDeleted) {
                savedSearchManager.deleteSavedSearch(subjectManager.getOverlord(), savedSearchId);
            }
        }
    }

}
