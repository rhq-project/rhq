package org.rhq.test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

public class JPAUtils {

    public static InitialContext getInitialContext() {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("java.naming.factory.initial", "org.jnp.interfaces.LocalOnlyContextFactory");
        env.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
        try {
            return new InitialContext(env);
        } catch (NamingException e) {
            throw new RuntimeException("Failed to load initial context", e);
        }
    }

    public static EntityManager lookupEntityManager() {
        try {
            InitialContext initialContext = getInitialContext();
            try {
                return ((EntityManagerFactory) initialContext.lookup("java:/RHQEntityManagerFactory"))
                    .createEntityManager();
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException("Failed to load entity manager", e);
        }
    }

    public static TransactionManager lookupTransactionManager() {
        try {
            InitialContext initialContext = getInitialContext();
            try {
                return (TransactionManager) initialContext.lookup("java:/TransactionManager");
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException("Failed to load transaction manager", e);
        }
    }

    public static void executeInTransaction(TransactionCallback callback) {
        TransactionManager tm = null;
        try {
            tm = lookupTransactionManager();
            tm.begin();
            callback.execute();
            tm.commit();
        } catch (Throwable t) {
            RuntimeException re = new RuntimeException(getAllThrowableMessages(t), t);
            try {
                if (tm != null) {
                    tm.rollback();
                }
            } catch (SystemException e) {
                throw new RuntimeException("Failed to rollback transaction ((" + getAllThrowableMessages(e)
                    + ")) but there was a real cause before this - see cause for that", re);
            }
            re.printStackTrace();
            throw re;
        }
    }

    public static <T> T executeInTransaction(TransactionCallbackWithContext<T> callback) {
        TransactionManager tm = null;
        try {
            tm = lookupTransactionManager();
            tm.begin();
            T results = callback.execute(tm, lookupEntityManager());
            tm.commit();
            return results;
        } catch (Throwable t) {
            RuntimeException re = new RuntimeException(getAllThrowableMessages(t), t);
            try {
                if (tm != null) {
                    tm.rollback();
                }
            } catch (SystemException e) {
                throw new RuntimeException("Failed to rollback transaction ((" + getAllThrowableMessages(e)
                    + ")) but there was a real cause before this - see cause for that", re);
            }
            re.printStackTrace();
            throw re;
        }
    }

    private static String getAllThrowableMessages(Throwable t) {
        ArrayList<String> list = new ArrayList<String>();
        if (t != null) {
            String msg = t.getClass().getName() + ":" + t.getMessage();
            if (t instanceof SQLException) {
                msg += "[SQLException=" + getAllSqlExceptionMessages((SQLException) t) + "]";
            }
            list.add(msg);

            while ((t.getCause() != null) && (t != t.getCause())) {
                t = t.getCause();
                msg = t.getClass().getName() + ":" + t.getMessage();
                if (t instanceof SQLException) {
                    msg += "[SQLException=" + getAllSqlExceptionMessages((SQLException) t) + "]";
                }
                list.add(msg);
            }
        }

        return list.toString();
    }

    private static String getAllSqlExceptionMessages(SQLException t) {
        ArrayList<String> list = new ArrayList<String>();
        if (t != null) {
            list.add(t.getClass().getName() + ":" + t.getMessage());
            while ((t.getNextException() != null) && (t != t.getNextException())) {
                t = t.getNextException();
                String msg = t.getClass().getName() + ":" + t.getMessage();
                list.add(msg + "(error-code=" + t.getErrorCode() + ",sql-state=" + t.getSQLState() + ")");
            }
        }
        return list.toString();
    }


}
