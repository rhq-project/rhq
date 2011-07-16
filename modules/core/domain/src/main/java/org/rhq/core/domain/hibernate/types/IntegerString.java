package org.rhq.core.domain.hibernate.types;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

public class IntegerString implements UserType {

    @Override
    public int[] sqlTypes() {
        return new int[] {Hibernate.INTEGER.sqlType()};
    }

    @Override
    public Class returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y) {
            return true;
        }
        if (x == null || y == null) {
            return false;
        }
        return x.equals(y);
    }

    @Override
    public int hashCode(Object o) throws HibernateException {
        return o.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet resultSet, String[] names, Object o) throws HibernateException, SQLException {
        int value = resultSet.getInt(names[0]);

        if (resultSet.wasNull()) {
            return null;
        }
        return Integer.toString(value);
    }

    @Override
    public void nullSafeSet(PreparedStatement stmt, Object value, int index) throws HibernateException,
        SQLException {
        if (value == null) {
            stmt.setNull(index, Hibernate.INTEGER.sqlType());
        } else {
            stmt.setInt(index, Integer.parseInt((String) value));
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}
