// SPDX-License-Identifier: Apache-2.0
// Originally developed by Telicent Ltd.; subsequently adapted, enhanced, and maintained by the National Digital Twin Programme.

/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 *  Modifications made by the National Digital Twin Programme (NDTP)
 *  © Crown Copyright 2025. This work has been developed by the National Digital Twin Programme
 *  and is legally attributed to the Department for Business and Trade (UK) as the governing entity.
 */
package uk.gov.dbt.ndtp.secure.agent.server.jaxrs.applications;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.JwkSetBuilder;
import io.jsonwebtoken.security.Jwks;
import java.security.Key;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import uk.gov.dbt.ndtp.secure.agent.server.jaxrs.resources.JwksResource;
import uk.gov.dbt.ndtp.servlet.auth.jwt.verifier.aws.AwsElbKeyUrlRegistry;

/**
 * A Mock Key Server that can supply both JSON Web Key Sets (JWKS) and individual keys in PEM format ala AWS ELB
 * <p>
 * Intended for use in unit test scenarios to aid in mocking out services with configured authentication by supplying
 * actual keys for use in both verification and signing
 * </p>
 */
public class MockKeyServer extends AbstractAppEntrypoint {

    private final int port;
    private final JwkSet privateKeys, publicKeys;
    private Object[][] keyIds;

    /**
     * Creates a new Mock Key Server running on the given port
     * <p>
     * Will automatically generate a JWKS with keys in a variety of algorithms and key lengths.  You can retrieve
     * </p>
     *
     * @param port Port the server should run on
     */
    public MockKeyServer(int port) {
        this.port = port;

        // Generate some Key Pairs using a variety of different algorithms
        List<KeyPair> keyPairs = List.of(Jwts.SIG.ES256.keyPair().build(), Jwts.SIG.ES384.keyPair().build(),
                                         Jwts.SIG.ES512.keyPair().build(), Jwts.SIG.RS256.keyPair().build(),
                                         Jwts.SIG.PS512.keyPair().build());
        JwkSetBuilder privateJwks = Jwks.set();
        JwkSetBuilder publicJwks = Jwks.set();
        keyPairs.forEach(p -> {
            privateJwks.add(Jwks.builder().keyPair(p).idFromThumbprint().build());
            publicJwks.add(Jwks.builder().key(p.getPublic()).idFromThumbprint().build());
        });
        this.privateKeys = privateJwks.build();
        this.publicKeys = publicJwks.build();
        JwksResource.setJwks(this.publicKeys);

        this.keyIds = new Object[keyPairs.size()][];
        for (int i = 0; i < this.publicKeys.getKeys().size(); i++) {
            this.keyIds[i] = new Object[] {
                    this.publicKeys.getKeys().stream().skip(i).map(k -> k.getId()).findFirst().orElse(null)
            };
        }
    }

    @Override
    protected ServerBuilder buildServer() {
        return ServerBuilder.create()
                            .application(MockKeyServerApplication.class)
                            .port(this.port)
                            .displayName("Mock Key Server");
    }

    /**
     * Registers this key server as the key resolver for an AWS region via the
     * {@link AwsElbKeyUrlRegistry#register(String, String)} method
     * <p>
     * When using in tests remember to call {@link AwsElbKeyUrlRegistry#reset()}  after your tests to remove this
     * configuration.
     * </p>
     *
     * @param region Region
     */
    public void registerAsAwsRegion(String region) {
        AwsElbKeyUrlRegistry.register(region, this.server.getBaseUri() + "%s");
    }

    /**
     * Gets the URL at which this key server returns the JWKS
     *
     * @return JWKS URL
     */
    public String getJwksUrl() {
        return this.server.getBaseUri() + "jwks.json";
    }

    /**
     * Gets the Key IDs as an object array, suitable for returning from a TestNG
     * {@link org.testng.annotations.DataProvider} annotated method in a test class
     *
     * @return Key IDs
     */
    public Object[][] getKeyIds() {
        return this.keyIds;
    }

    /**
     * Gets the Key IDs as a list
     *
     * @return Key IDs
     */
    public List<String> getKeyIdsAsList() {
        List<String> ids = new ArrayList<>();
        for (Object[] keyId : this.keyIds) {
            ids.add((String) keyId[0]);
        }
        return ids;
    }

    /**
     * Gets a private key by ID, thus allowing tests to sign JWTs with a private key that can later be validated by a
     * {@link uk.gov.dbt.ndtp.servlet.auth.jwt.verification.jwks.UrlJwksKeyLocator} or
     * {@link uk.gov.dbt.ndtp.servlet.auth.jwt.verifier.aws.AwsElbKeyResolver} configured to point to this mock server
     *
     * @param keyId Key ID
     * @return Private Key
     * @throws NullPointerException If the Key ID is not valid, see {@link #getKeyIdsAsList()} to find the available Key
     *                              IDs
     */
    public Key getPrivateKey(String keyId) {
        return this.privateKeys.getKeys()
                               .stream()
                               .filter(k -> Objects.equals(k.getId(), keyId))
                               .findFirst()
                               .get()
                               .toKey();
    }

    /**
     * Starts the mock key server
     */
    public void start() {
        this.run(false);
    }

    /**
     * Stops the mock key server
     */
    public void stop() {
        this.server.shutdownNow();
    }

}
