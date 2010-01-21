package org.rhq.core.domain.content.transfer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class EntitlementCertificate {

    private String certificate;
    private String key;

    /**
     * Get certificate for even numbered resource ids.
     * @return The cert.
     * @note This is a prototype hack.
     */
    public static EntitlementCertificate getOdd() {
        DummyLoader loader = new DummyLoader();
        try {
            return loader.getOdd();
        } catch (Exception ex) {
        }
        return null;
    }

    /**
     * Get certificate for odd numbered resource ids.
     * @return The cert.
     * @note This is a prototype hack.
     */
    public static EntitlementCertificate getEven() {
        DummyLoader loader = new DummyLoader();
        try {
            return loader.getEven();
        } catch (Exception ex) {
        }
        return null;
    }

    /**
     *
     */
    public EntitlementCertificate() {

    }

    /**
     *
     * @param certificate
     * @param key
     */
    public EntitlementCertificate(String certificate, String key) {
        this.certificate = certificate;
        this.key = key;
    }

    /**
     * @return the certificate
     */
    public String getCertificate() {
        return certificate;
    }

    /**
     * @param certificate the certificate to set
     */
    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    /**
     * @return the private key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the private key to set
     */
    public void setKey(String key) {
        this.key = key;
    }
}

class DummyLoader {

    private static final String dir = "/etc/pki/yum/rhq";

    EntitlementCertificate getOdd() throws IOException {
        String key = read("odd.key");
        String pem = read("odd.pem");
        return new EntitlementCertificate(key, pem);
    }

    EntitlementCertificate getEven() throws IOException {
        String key = read("even.key");
        String pem = read("even.pem");
        return new EntitlementCertificate(key, pem);
    }

    private String read(String fn) throws IOException {
        String path = dir + File.separator + fn;
        File f = new File(path);
        byte[] buffer = new byte[(int) f.length()];
        BufferedInputStream istr = new BufferedInputStream(new FileInputStream(f));
        istr.read(buffer);
        String result = new String(buffer);
        istr.close();
        return result;
    }
}
