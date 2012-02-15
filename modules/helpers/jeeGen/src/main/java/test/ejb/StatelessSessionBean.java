package test.ejb;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

/**
 * Stateless session bean - impl.
 */
public class StatelessSessionBean implements SessionBean {

    private static final long serialVersionUID = 1L;

    private SessionContext context;

    public String echo(String string) {
        return string;
    }

    public void ejbCreate() {
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }

    public void ejbRemove() {
    }

    public void setSessionContext(SessionContext context) {
        this.context = context;
    }

}