/**
 * 
 */
package org.rhq.plugins.antlrconfig.tokens;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.Token;

/**
 * A token class used to skip the token when working with a tree.
 * 
 * @author Lukas Krejci
 */
public class Skip extends RhqConfigToken {

    private static final long serialVersionUID = 1L;

    /**
     * @param input
     * @param type
     * @param channel
     * @param start
     * @param stop
     */
    public Skip(CharStream input, int type, int channel, int start, int stop) {
        super(input, type, channel, start, stop);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param type
     * @param text
     */
    public Skip(int type, String text) {
        super(type, text);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param type
     */
    public Skip(int type) {
        super(type);
        // TODO Auto-generated constructor stub
    }

    /**
     * @param oldToken
     */
    public Skip(Token oldToken) {
        super(oldToken);
        // TODO Auto-generated constructor stub
    }

}
