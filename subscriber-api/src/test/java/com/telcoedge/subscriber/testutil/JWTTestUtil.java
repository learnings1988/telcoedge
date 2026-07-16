package com.telcoedge.subscriber.testutil;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.testcontainers.shaded.org.bouncycastle.est.LimitedSource;
import org.testcontainers.shaded.org.bouncycastle.jcajce.provider.asymmetric.RSA;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

public class JWTTestUtil {

    private static final RSAPrivateKey PRIVATE_KEY = loadPrivateKey();

    public JWTTestUtil() {
    }

    private static RSAPrivateKey loadPrivateKey()  {
        try(InputStream is = JWTTestUtil.class.getResourceAsStream("/keys/private.pem")){
            String pem = new String(is.readAllBytes())
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        }catch (Exception e){
            throw new RuntimeException("Failed to load test private Key" + e);
        }
    }


    public static String generateToken(String subject, String operatorid, List<String> roles){
        try{
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer("telcoedge-test")
                    .claim("operator_id" , operatorid)
                    .claim("roles", roles)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(3600)))
                    .build();

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.RS256),claims);

            jwt.sign( new RSASSASigner(PRIVATE_KEY));
            return jwt.serialize();
        }catch(Exception e){
            throw new RuntimeException("Failed to generate JWT token" , e);
        }
    }


    public static String operatorToken(String operatorId){
        return generateToken("api-user", operatorId, List.of("ROLE_OPERATOR"));
    }
}
