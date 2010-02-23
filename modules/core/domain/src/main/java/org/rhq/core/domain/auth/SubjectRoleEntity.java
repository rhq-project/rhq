package org.rhq.core.domain.auth;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.rhq.core.domain.authz.Role;

@Table(name = "RHQ_SUBJECT_ROLE_MAP")
public class SubjectRoleEntity implements Serializable {

    @ManyToOne
    @JoinColumn(name = "SUBJECT_ID", referencedColumnName = "ID")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "ROLE_ID", referencedColumnName = "ID")
    private Role role;

    @Column(name = "IS_LDAP")
    private boolean isLdap;

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isLdap() {
        return isLdap;
    }

    public void setLdap(boolean isLdap) {
        this.isLdap = isLdap;
    }

}
