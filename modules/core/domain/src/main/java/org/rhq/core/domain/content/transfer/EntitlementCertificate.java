package org.rhq.core.domain.content.transfer;

public class EntitlementCertificate {

    private String name;
    private String certificate;
    private String key;

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
    public EntitlementCertificate(String name, String certificate, String key) {
        this.name = name;
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
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
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