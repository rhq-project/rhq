package org.rhq.core.gui.configuration;

import javax.el.ELContext;
import javax.el.ValueExpression;

public class MockValueExpression extends ValueExpression {

    @Override
    public Class<?> getExpectedType() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Class<?> getType(ELContext arg0) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Object getValue(ELContext arg0) {
        return value;
    }

    @Override
    public boolean isReadOnly(ELContext arg0) {
        throw new RuntimeException("Function not implemented");

    }

    public Object value;
    public boolean literalText = false;

    @Override
    public void setValue(ELContext arg0, Object arg1) {
        this.value = arg1;
    }

    @Override
    public boolean equals(Object arg0) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public String getExpressionString() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public int hashCode() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public boolean isLiteralText() {
        return literalText;
    }

}
