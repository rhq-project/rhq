package org.rhq.plugins.apache.parser;


public class ApacheParserException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    public ApacheParserException() {
        super();
    }

    /**
     * @param message
     * @param cause
     */
    public ApacheParserException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public ApacheParserException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public ApacheParserException(Throwable cause) {
        super(cause);
    }

    
}
