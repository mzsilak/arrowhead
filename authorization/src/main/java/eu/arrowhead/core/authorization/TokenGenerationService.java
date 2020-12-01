/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.core.authorization;

import eu.arrowhead.common.Utility;
import eu.arrowhead.common.database.ArrowheadCloud;
import eu.arrowhead.common.database.ArrowheadSystem;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.messages.ArrowheadToken;
import eu.arrowhead.common.messages.RawTokenInfo;
import eu.arrowhead.common.messages.TokenGenerationRequest;
import eu.arrowhead.common.misc.SecurityUtils;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.ServiceConfigurationError;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.ws.rs.core.Response.Status;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

class TokenGenerationService {

  private static final Logger log = LogManager.getLogger(TokenGenerationService.class.getName());

  static List<ArrowheadToken> generateTokens(TokenGenerationRequest request) {
    // First get the public key for each provider
    List<PublicKey> publicKeys = getProviderPublicKeys(request.getProviders());

    // Cryptographic object initializations
    Security.addProvider(new BouncyCastleProvider());
    Cipher cipher;
    try {
      cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding", "BC");
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | NoSuchProviderException e) {
      log.fatal("Cipher.getInstance(String) throws exception, code needs to be changed!");
      throw new AssertionError("Cipher.getInstance(String) throws exception, code needs to be changed!", e);
    }
    Signature signature;
    try {
      signature = Signature.getInstance("SHA256withRSA", "BC");
      signature.initSign(AuthorizationMain.privateKey);
    } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
      log.fatal("Signature.getInstance(String) throws exception, code needs to be changed!");
      throw new AssertionError("Signature.getInstance(String) throws exception, code needs to be changed!", e);
    } catch (InvalidKeyException e) {
      log.fatal("The private key of the Authorization module is invalid, keystore needs to be changed!");
      throw new ServiceConfigurationError("The private key of the Authorization module is invalid, keystore needs to be changed!", e);
    }

    // Create the ArrowheadToken for each provider
    RawTokenInfo rawTokenInfo = new RawTokenInfo();
    List<ArrowheadToken> tokens = new ArrayList<>();
    for (PublicKey key : publicKeys) {
      // Can not generate token without the provider public key
      if (key == null) {
        tokens.add(null);
        continue;
      }

      // Set consumer info string
      String c = request.getConsumer().getSystemName();
      if (request.getConsumerCloud() != null) {
        c = c.concat(".").concat(request.getConsumerCloud().getCloudName()).concat(".").concat(request.getConsumerCloud().getOperator());
      } else {
        boolean secureMode = Boolean.valueOf(System.getProperty("is_secure", "false"));
        ArrowheadCloud ownCloud = Utility.getOwnCloud(secureMode);
        c = c.concat(".").concat(ownCloud.getCloudName()).concat(".").concat(ownCloud.getOperator());
      }
      rawTokenInfo.setC(c);

      String s = request.getService().getServiceDefinition();
      // Set service info string
      List<String> interfaces = new ArrayList<>(request.getService().getInterfaces());
      if (!interfaces.isEmpty()) {
        s = interfaces.get(0) + "." + s;
      }
      rawTokenInfo.setS(s);

      // Set the token validity duration
      if (request.getDuration() != 0) {
        long endTime = System.currentTimeMillis() + request.getDuration();
        rawTokenInfo.setE(endTime);
      } else {
        // duration = 0 means a token is valid without a time limitation
        rawTokenInfo.setE(0L);
      }

      // There is an upper limit for the size of the token info, skip providers which exceeds this limit
      String json = Utility.toPrettyJson(null, rawTokenInfo);
      if (json == null) {
        log.error("RawTokenInfo serialization failed. Skipped provider.");
        continue;
      }
      System.out.println("Raw token info: ");
      System.out.println(json);
      if (json.length() > 244) {
        tokens.add(null);
        log.error("ArrowheadToken exceeded the size limit. Skipped provider.");
        continue;
      }

      // Finally, generate the token and signature strings
      try {
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] tokenBytes = cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
        System.out.println("Token bytes: " + Arrays.toString(tokenBytes));
        signature.update(tokenBytes);
        byte[] sigBytes = signature.sign();
        System.out.println("Signature bytes: " + Arrays.toString(sigBytes));

        String tokenString = Base64.getEncoder().encodeToString(tokenBytes);
        String signatureString = Base64.getEncoder().encodeToString(sigBytes);
        tokens.add(new ArrowheadToken(tokenString, signatureString));
      } catch (Exception e) {
        e.printStackTrace();
        log.error("Cipher or Signature class throws public key specific exception: " + e.getMessage());
        tokens.add(null);
      }
    }

    // Throw an exception if none of the token generation was successful
    boolean nonNullTokenExists = false;
    for (ArrowheadToken token : tokens) {
      if (token != null) {
        nonNullTokenExists = true;
        break;
      }
    }
    if (!nonNullTokenExists) {
      log.error("None of the provider ArrowheadSystems in this orchestration have a valid RSA public key spec stored in the database.");
      throw new ArrowheadException("Token generation failed for all the provider ArrowheadSystems.", Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    return tokens;
  }


  private static List<PublicKey> getProviderPublicKeys(List<ArrowheadSystem> providers) {
    List<PublicKey> keys = new ArrayList<>();

    for (ArrowheadSystem provider : providers) {
      try {
        PublicKey key = SecurityUtils.getPublicKey(provider.getAuthenticationInfo(), false);
        keys.add(key);
      } catch (AuthException e) {
        log.error("The stored auth info for the ArrowheadSystem (" + provider.getSystemName()
                      + ") is not a proper RSA public key spec, or it is incorrectly encoded, or missing. The public key can not be decoded from "
                      + "it.");
        keys.add(null);
      }
    }

    // Throw an exception if none of the public keys could be acquired from the specs
    boolean nonNullKeyExists = false;
    for (PublicKey key : keys) {
      if (key != null) {
        nonNullKeyExists = true;
        break;
      }
    }
    if (!nonNullKeyExists) {
      log.error("None of the provider ArrowheadSystems in this orchestration have a valid RSA public key spec stored in the database.");
      throw new ArrowheadException("Token generation failed for all the provider ArrowheadSystems.", Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }

    return keys;
  }

}