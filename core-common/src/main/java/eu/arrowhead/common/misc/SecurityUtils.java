/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.misc;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;
import java.util.regex.Pattern;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unused")
public final class SecurityUtils {

  private static final Logger log = LogManager.getLogger(SecurityUtils.class.getName());

  public static KeyStore loadKeyStore(String filePath, String pass) {
    try {
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      InputStream is = new FileInputStream(filePath);
      keystore.load(is, pass.toCharArray());
      is.close();
      return keystore;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      log.fatal("Loading the keystore failed: " + e.toString() + " " + e.getMessage());
      throw new ServiceConfigurationError("Loading the keystore failed...", e);
    }
  }

  public static X509Certificate getFirstCertFromKeyStore(KeyStore keystore) {
    try {
      Enumeration<String> enumeration = keystore.aliases();
      String alias = enumeration.nextElement();
      return (X509Certificate) keystore.getCertificate(alias);
    } catch (KeyStoreException | NoSuchElementException e) {
      log.error("Getting the first cert from keystore failed: " + e.toString() + " " + e.getMessage());
      throw new ServiceConfigurationError("Getting the first cert from keystore failed...", e);
    }
  }

  public static String getFirstAliasFromKeyStore(KeyStore keyStore) {
    try {
      Enumeration<String> aliases = keyStore.aliases();
      return aliases.nextElement();
    } catch (KeyStoreException e) {
      log.error("Getting the first alias from keystore failed: " + e.toString() + " " + e.getMessage());
      throw new ServiceConfigurationError("Getting the first alias from keystore failed...", e);
    }
  }

  public static String getCertCNFromSubject(String subjectName) {
    String cn = null;
    try {
      // Subject is in LDAP format, we can use the LdapName object for parsing
      LdapName ldapname = new LdapName(subjectName);
      for (Rdn rdn : ldapname.getRdns()) {
        // Find the data after the CN field
        if (rdn.getType().equalsIgnoreCase("CN")) {
          cn = (String) rdn.getValue();
        }
      }
    } catch (InvalidNameException e) {
      System.out.println("InvalidNameException in getCertCNFromSubject: " + e.getMessage());
      return "";
    }

    if (cn == null) {
      return "";
    }

    return cn;
  }

  public static PrivateKey getPrivateKey(KeyStore keystore, String pass) {
    PrivateKey privatekey = null;
    String element;
    try {
      Enumeration<String> enumeration = keystore.aliases();
      while (enumeration.hasMoreElements()) {
        element = enumeration.nextElement();
        privatekey = (PrivateKey) keystore.getKey(element, pass.toCharArray());
        if (privatekey != null) {
          break;
        }
      }
    } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
      log.error("Getting the private key from keystore failed: " + e.toString() + " " + e.getMessage());
      throw new ServiceConfigurationError("Getting the private key from keystore failed...", e);
    }

    if (privatekey == null) {
      throw new ServiceConfigurationError("Getting the private key failed, keystore aliases do not identify a key.");
    }
    return privatekey;
  }

  public static KeyStore createKeyStoreFromCert(String filePath, String alias) {
    try {
      InputStream is = new FileInputStream(filePath);
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      X509Certificate cert = (X509Certificate) cf.generateCertificate(is);

      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(null); // We don't need the KeyStore instance to come from a file.
      keystore.setCertificateEntry(alias, cert);
      return keystore;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
      log.fatal("Keystore creation from cert failed: " + e.toString() + " " + e.getMessage());
      throw new ServiceConfigurationError("Keystore creation from cert failed...", e);
    }
  }

  public static SSLContext createMasterSSLContext(String keyStorePath, String keyStorePass, String keyPass, String masterArrowheadCertPath) {
    try {
      KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePass);
      String kmfAlgorithm = System.getProperty("ssl.KeyManagerFactory.algorithm", KeyManagerFactory.getDefaultAlgorithm());
      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(kmfAlgorithm);
      keyManagerFactory.init(keyStore, keyPass != null ? keyPass.toCharArray() : keyStorePass.toCharArray());

      KeyStore trustStore = createKeyStoreFromCert(masterArrowheadCertPath, "arrowhead.eu");
      String tmfAlgorithm = System.getProperty("ssl.TrustManagerFactory.algorithm", TrustManagerFactory.getDefaultAlgorithm());
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
      trustManagerFactory.init(trustStore);

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
      return sslContext;
    } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException | KeyManagementException e) {
      log.fatal("Master SSLContext creation failed: " + e.toString() + " " + e.getMessage());
      throw new ServiceConfigurationError("Master SSLContext creation failed...", e);
    }
  }

  public static SSLContext createSSLContextWithDummyTrustManager(String keyStorePath, String keyStorePass) {
    try {
      KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePass);
      String kmfAlgorithm = System.getProperty("ssl.KeyManagerFactory.algorithm", KeyManagerFactory.getDefaultAlgorithm());
      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(kmfAlgorithm);
      keyManagerFactory.init(keyStore, keyStorePass.toCharArray());

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagerFactory.getKeyManagers(), createTrustManagers(), null);
      return sslContext;
    } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException | KeyManagementException e) {
      log.fatal("SSLContext (with dummy trust manager) creation failed: " + e.toString() + " " + e.getMessage());
      throw new ServiceConfigurationError("AcceptAll SSLContext creation failed...", e);
    }
  }

  public static boolean isKeyStoreCNArrowheadValid(String commonName) {
    String[] cnFields = commonName.split("\\.", 0);
    return cnFields.length == 5 && cnFields[3].equals("arrowhead") && cnFields[4].equals("eu");
  }

  public static boolean isKeyStoreCNArrowheadValid(String clientCN, String cloudCN) {
    String[] clientFields = clientCN.split("\\.", 2);
    return clientFields.length == 2 && cloudCN.equalsIgnoreCase(clientFields[1]);
  }

  public static boolean isTrustStoreCNArrowheadValid(String commonName) {
    String[] cnFields = commonName.split("\\.", 0);
    return cnFields.length == 4 && cnFields[2].equals("arrowhead") && cnFields[3].equals("eu");
  }

  public static boolean isKeyStoreCNArrowheadValidLegacy(String commonName) {
    String[] cnFields = commonName.split("\\.", 0);
    return cnFields.length == 6 && cnFields[3].equals("arrowhead") && cnFields[4].equals("eu");
  }

  public static X509Certificate getCertFromKeyStore(KeyStore keystore, String name) {
    Enumeration<String> enumeration;
    try {
      enumeration = keystore.aliases();
    } catch (KeyStoreException e) {
      log.error("getCertFromKeyStore throws KeyStoreException");
      return null;
    }

    while (enumeration.hasMoreElements()) {
      String alias = enumeration.nextElement();

      X509Certificate clientCert;
      try {
        clientCert = (X509Certificate) keystore.getCertificate(alias);
      } catch (KeyStoreException e) {
        log.error("getCertFromKeyStore throws KeyStoreException when loading cert: " + alias);
        continue;
      }
      String clientCertCN = getCertCNFromSubject(clientCert.getSubjectDN().getName());

      if (!clientCertCN.equals(name)) {
        continue;
      }
      return clientCert;
    }

    return null;
  }

  public static String getKeyEncoded(Key key) {
    if (key == null) {
      return "";
    }

    byte[] encpub = key.getEncoded();
    StringBuilder sb = new StringBuilder(encpub.length * 2);
    for (byte b : encpub) {
      sb.append(String.format("%02x", b & 0xff));
    }
    return sb.toString();
  }

  public static String getByteEncoded(byte[] array) {
    StringBuilder sb = new StringBuilder(array.length * 2);
    for (byte b : array) {
      sb.append(String.format("%02X", b & 0xff));
    }
    return sb.toString();
  }

  /**
   * Extract a public key either from a PEM encoded file or directly from the Base64 coded string.
   *
   * @param filePathOrEncodedKey either a file path for the PEM file or the Base64 encoded key
   * @param isFilePath true if the first parameter is a file path
   *
   * @return the PublicKey
   */
  public static PublicKey getPublicKey(String filePathOrEncodedKey, boolean isFilePath) {
    byte[] keyBytes;
    if (isFilePath) {
      keyBytes = loadPEM(filePathOrEncodedKey);
    } else {
      try {
        keyBytes = Base64.getDecoder().decode(filePathOrEncodedKey);
      } catch (IllegalArgumentException | NullPointerException e) {
        throw new AuthException("Public key decoding failed! Caused by: " + e.getMessage(), e);
      }
    }
    try {
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePublic(new X509EncodedKeySpec(keyBytes));
    } catch (NoSuchAlgorithmException e) {
      log.fatal("KeyFactory.getInstance(String) throws NoSuchAlgorithmException, code needs to be changed!");
      throw new AssertionError("KeyFactory.getInstance(String) throws NoSuchAlgorithmException, code needs to be changed!", e);
    } catch (InvalidKeySpecException e) {
      log.error("getPublicKey: X509 keyspec could not be created from the decoded bytes.");
      throw new AuthException("PublicKey decoding failed due wrong input key", e);
    }
  }

  public static byte[] loadPEM(String filePath) {
    try (FileInputStream in = new FileInputStream(filePath)) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] buf = new byte[1024];
      for (int read = 0; read != -1; read = in.read(buf)) {
        baos.write(buf, 0, read);
      }
      String pem = new String(baos.toByteArray(), StandardCharsets.ISO_8859_1);
      baos.close();
      Pattern parse = Pattern.compile("(?m)(?s)^---*BEGIN.*---*$(.*)^---*END.*---*$.*");
      String encoded = parse.matcher(pem).replaceFirst("$1");
      return Base64.getMimeDecoder().decode(encoded);
    } catch (IOException e) {
      throw new ArrowheadException("IOException occurred during PEM file loading from " + filePath, e);
    }
  }

  private static TrustManager[] createTrustManagers() {
    return new TrustManager[]{new X509TrustManager() {

      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new java.security.cert.X509Certificate[]{};
      }

      public void checkClientTrusted(X509Certificate[] chain, String authType) {
      }

      public void checkServerTrusted(X509Certificate[] chain, String authType) {
      }
    }};
  }

}
