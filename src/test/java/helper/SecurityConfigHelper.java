package helper;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class SecurityConfigHelper {

    private static final RSAKey SIGNING_KEY = signingKey();

    private SecurityConfigHelper() {
    }

    public static String signedToken(
        String issuer, List<String> audience, Map<String, Object> realmAccess) {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("security-test-organizer")
                .audience(audience)
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("realm_access", realmAccess)
                .build();
            try {
                SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(SIGNING_KEY.getKeyID())
                        .build(),
                    claims);
                jwt.sign(new RSASSASigner(SIGNING_KEY.toPrivateKey()));
                return jwt.serialize();
            } catch (JOSEException exception) {
                throw new IllegalStateException(exception);
            }
    }

    private static RSAKey signingKey() {
            try {
                return new RSAKeyGenerator(2048).keyID("security-config-test-key").generate();
            } catch (JOSEException exception) {
                throw new ExceptionInInitializerError(exception);
            }
    }

    public static HttpServer startJwksServer() {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                byte[] jwks = new JWKSet(SIGNING_KEY.toPublicJWK())
                    .toString()
                    .getBytes(StandardCharsets.UTF_8);
                server.createContext(
                    "/jwks",
                    exchange -> {
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, jwks.length);
                        exchange.getResponseBody().write(jwks);
                        exchange.close();
                    });
                server.start();
                return server;
            } catch (IOException exception) {
                throw new ExceptionInInitializerError(exception);
            }
    }
}
