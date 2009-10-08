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
package org.rhq.enterprise.server.core.jaas;

import java.security.acl.Group;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.security.SimpleGroup;
import org.jboss.security.auth.spi.UsernamePasswordLoginModule;
import org.rhq.enterprise.server.RHQConstants;

/**
 * A JDBC login module that only supports authentication JDBC LoginModule options:
 *
 * <pre>
 * principalsQuery
 *   Query used to extract the password for a given user.  By default
 *   this value is "SELECT password FROM principals WHERE principal=?"
 *
 * dsJndiName
 *   JNDI name of the datasource to use.  Default value is java:/HypericDS
 * </pre>
 */

public class JDBCLoginModule extends UsernamePasswordLoginModule {
    private Log log = LogFactory.getLog(JDBCLoginModule.class);
    private String dsJndiName;
    private String principalsQuery = "SELECT password FROM RHQ_PRINCIPAL WHERE principal=?";

    /**
     * @see UsernamePasswordLoginModule#initialize(Subject, CallbackHandler, Map, Map)
     */
    @Override
    public void initialize(Subject subj, CallbackHandler handler, Map shared_state, Map opts) {
        super.initialize(subj, handler, shared_state, opts);

        dsJndiName = (String) opts.get("dsJndiName");
        if (dsJndiName == null) {
            dsJndiName = RHQConstants.DATASOURCE_JNDI_NAME;
        }

        Object tmpQuery = opts.get("principalsQuery");

        if (tmpQuery != null) {
            principalsQuery = tmpQuery.toString();
        }

        log.debug("dsJndiName=" + dsJndiName);
        log.debug("prinipalsQuery=" + principalsQuery);
    }

    private Properties getProperties() {
        Properties props = new Properties();

        props.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
        props.put("java.naming.provider.url", "jnp://localhost:1099");
        props.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");

        return props;
    }

    /**
     * @see org.jboss.security.auth.spi.UsernamePasswordLoginModule#getUsersPassword()
     */
    @Override
    protected String getUsersPassword() throws LoginException {
        String username = getUsername();
        String password = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Properties props = getProperties();
            InitialContext ctx = new InitialContext(props);
            DataSource ds = (DataSource) ctx.lookup(dsJndiName);
            conn = ds.getConnection();

            ps = conn.prepareStatement(principalsQuery);
            ps.setString(1, username);
            rs = ps.executeQuery();
            if (rs.next() == false) {
                throw new FailedLoginException("No matching username found in principals");
            }

            password = rs.getString(1);
        } catch (NamingException ex) {
            throw new LoginException(ex.toString(true));
        } catch (SQLException ex) {
            throw new LoginException(ex.toString());
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }

            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ex) {
                }
            }
        }

        return password;
    }

    /**
     * @see org.jboss.security.auth.spi.AbstractServerLoginModule#getRoleSets()
     */
    @Override
    protected Group[] getRoleSets() throws LoginException {
        SimpleGroup roles = new SimpleGroup("Roles");

        //roles.addMember( new SimplePrincipal( "some user" ) );
        Group[] roleSets = { roles };
        return roleSets;
    }
}