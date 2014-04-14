/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.core.util.obfuscation;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class makes available methods for obfuscating a string in the very same way
 * as the <code>org.jboss.resource.security.SecureIdentityLoginModule</code> in JBossAS 4.2.3.
 * <p>
 * This is to ensure backwards compatibility in case we switch containers that would start
 * obfuscating the password in a different way and also to make those methods available to
 * other code. The original methods in the SecureIdentityLoginModule are marked private.
 *
 * @author Lukas Krejci
 */
public final class Obfuscator {

    private static final byte[] KEY = "jaas is the way".getBytes();
    public static final String ALGORITHM = "Blowfish";

    //no instances, please
    private Obfuscator() {

    }

    /**
     * Encodes the secret string so that the value is not immediately readable by
     * a "casual viewer".
     *
     * @param secret the string to encode
     * @return encoded string
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public static String encode(String secret) throws NoSuchPaddingException, NoSuchAlgorithmException,
        InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        if (secret == null) {
            return null;
        }

        SecretKeySpec key = new SecretKeySpec(KEY, ALGORITHM);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encoding = cipher.doFinal(secret.getBytes());
        BigInteger n = new BigInteger(encoding);
        return n.toString(16);
    }

    /**
     * Decodes the string obfuscated using the {@link #encode(String)} method back to the
     * original value.
     * <p>
     * This method differs from its original <code>org.jboss.resource.security.SecureIdentityLoginModule#decode</code>
     * private method in that it returns a String whereas the original method returns a char[].
     *
     * @param secret the encoded (obfuscated) string
     * @return the decoded string
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    public static String decode(String secret) throws NoSuchPaddingException, NoSuchAlgorithmException,
        InvalidKeyException, BadPaddingException, IllegalBlockSizeException {

        if (secret == null) {
            return null;
        }

        SecretKeySpec key = new SecretKeySpec(KEY, ALGORITHM);

        BigInteger n = new BigInteger(secret, 16);
        byte[] encoding = n.toByteArray();

        //SECURITY-344: fix leading zeros
        if (encoding.length % 8 != 0) {
            int length = encoding.length;
            int newLength = ((length / 8) + 1) * 8;
            int pad = newLength - length; //number of leading zeros
            byte[] old = encoding;
            encoding = new byte[newLength];
            for (int i = old.length - 1; i >= 0; i--) {
                encoding[i + pad] = old[i];
            }
            //SECURITY-563: handle negative numbers
            if (n.signum() == -1) {
                for (int i = 0; i < newLength - length; i++) {
                    encoding[i] = (byte) -1;
                }
            }
        }

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decode = cipher.doFinal(encoding);
        return new String(decode);
    }

    /**
     * Adapted from http://stackoverflow.com/questions/2863852/how-to-generate-a-random-string-in-java.
     * <p/>
     * The default set of validCharacters: 1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ
     *
     * @param random
     * @param validCharacters
     * @param length
     * @return
     */
    public static String generateString(Random random, String validCharacters, int length) {
        validCharacters = (null == validCharacters || validCharacters.isEmpty()) ? "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            : validCharacters;
        length = (length < 1) ? 10 : length;

        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = validCharacters.charAt(random.nextInt(validCharacters.length()));
        }
        return new String(text);
    }
}
