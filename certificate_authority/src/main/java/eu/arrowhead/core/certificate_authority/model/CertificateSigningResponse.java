/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.certificate_authority.model;

public class CertificateSigningResponse {

  private String encodedSignedCert;
  private String intermediateCert;
  private String rootCert;

  public CertificateSigningResponse() {
  }

  public CertificateSigningResponse(String encodedSignedCert) {
    this.encodedSignedCert = encodedSignedCert;
  }

  public CertificateSigningResponse(String encodedSignedCert, String intermediateCert, String rootCert) {
    this.encodedSignedCert = encodedSignedCert;
    this.intermediateCert = intermediateCert;
    this.rootCert = rootCert;
  }

  public String getEncodedSignedCert() {
    return encodedSignedCert;
  }

  public void setEncodedSignedCert(String encodedSignedCert) {
    this.encodedSignedCert = encodedSignedCert;
  }

  public String getIntermediateCert() {
    return intermediateCert;
  }

  public void setIntermediateCert(String intermediateCert) {
    this.intermediateCert = intermediateCert;
  }

  public String getRootCert() {
    return rootCert;
  }

  public void setRootCert(String rootCert) {
    this.rootCert = rootCert;
  }
}
