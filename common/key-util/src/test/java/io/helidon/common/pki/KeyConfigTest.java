/*
 * Copyright (c) 2017, 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.common.pki;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Optional;

import io.helidon.common.configurable.Resource;
import io.helidon.common.configurable.ResourceException;
import io.helidon.config.Config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for {@link io.helidon.common.pki.Keys}.
 */
class KeyConfigTest {
    private static Config config;

    @BeforeAll
    static void init() {
        config = Config.create();
    }

    @Test
    void testConfigPublicKey() {
        Keys publicKey = Keys.create(config.get("unit-1"));

        assertThat(publicKey.certChain().size(), is(0));
        assertThat(publicKey.privateKey().isPresent(), is(false));
        assertThat(publicKey.publicCert().isPresent(), is(true));
        assertThat(publicKey.publicKey().isPresent(), is(true));
    }

    @ParameterizedTest
    @CsvSource({"512", "1024", "2048"})
    void testPkcs1(String length) {
        PrivateKey privateKey = PemReader.readPrivateKey(KeyConfigTest.class.getResourceAsStream("/keystore/pkcs1-" + length +
                                                                                                         ".pem"), null);
        assertThat(privateKey, notNullValue());
        assertThat(privateKey.getAlgorithm(), is("RSA"));
    }

    @Test
    void testOldPublicKey() {
        Keys publicKey = Keys.create(config.get("unit-2"));

        assertThat("Certificate chain should be empty", publicKey.certChain().size(), is(0));
        assertThat("Private key should be empty", publicKey.privateKey().isPresent(), is(false));
        assertThat("Public certificate should be present", publicKey.publicCert().isPresent(), is(true));
        assertThat("Public key should be present", publicKey.publicKey().isPresent(), is(true));
    }

    @Test
    void testConfigPrivateKey() {
        Keys keyConfig = Keys.create(config.get("unit-3"));

        assertThat(keyConfig.certChain().size(), is(0));
        assertThat(keyConfig.privateKey().isPresent(), is(true));
        assertThat(keyConfig.publicCert().isPresent(), is(false));
        assertThat(keyConfig.publicKey().isPresent(), is(false));
    }

    @Test
    void testConfigCertChain() {
        Keys publicKey = Keys.create(config.get("unit-6"));

        assertThat(publicKey.certChain().size(), is(1));
        // private key is loaded by default, as it uses the alias "1"
        assertThat(publicKey.privateKey().isPresent(), is(true));
        // public cert is loaded by default from cert chain
        assertThat(publicKey.publicCert().isPresent(), is(true));
        assertThat(publicKey.publicKey().isPresent(), is(true));
    }

    @Test
    void testConfigWrongPath() {
        assertThrows(ResourceException.class, () -> Keys.create(config.get("unit-7")));
    }

    @Test
    void testConfigPartInvalid() {
        Keys invalid = Keys.create(config.get("unit-8"));
        assertThat(invalid.privateKey().isPresent(), is(false));
        assertThat(invalid.publicCert().isPresent(), is(false));
        assertThat(invalid.publicKey().isPresent(), is(false));
        assertThat(invalid.certChain().isEmpty(), is(true));
    }

    @Test
    void testConfigInvalid() {
        Keys invalid = Keys.create(config.get("unit-9"));
        assertThat(invalid.privateKey().isPresent(), is(false));
        assertThat(invalid.publicCert().isPresent(), is(false));
        assertThat(invalid.publicKey().isPresent(), is(false));
        assertThat(invalid.certChain().isEmpty(), is(true));
    }

    @Test
    void testResourcePath() {
        Keys keyConfig = Keys.create(config.get("unit-4"));

        assertThat(keyConfig.certChain().size(), is(0));
        assertThat(keyConfig.privateKey().isPresent(), is(true));
        assertThat(keyConfig.publicCert().isPresent(), is(false));
        assertThat(keyConfig.publicKey().isPresent(), is(false));
    }

    @Test
    void testContent() {
        Keys keyConfig = Keys.create(config.get("unit-10"));

        assertThat(keyConfig.certChain().size(), is(0));
        assertThat(keyConfig.privateKey().isPresent(), is(true));
        assertThat(keyConfig.publicCert().isPresent(), is(false));
        assertThat(keyConfig.publicKey().isPresent(), is(false));
    }

    @Test
    void testDoublePath() {
        Keys keyConfig = Keys.create(config.get("unit-5"));

        assertThat(keyConfig.certChain().size(), is(0));
        assertThat(keyConfig.privateKey().isPresent(), is(true));
        assertThat(keyConfig.publicCert().isPresent(), is(false));
        assertThat(keyConfig.publicKey().isPresent(), is(false));
    }

    @Test
    void testPem() {
        Keys conf = Keys.builder()
                .pem(pemBuilder -> pemBuilder.certChain(Resource.create("keystore/public_key_cert.pem"))
                        .key(Resource.create("keystore/id_rsa.p8"))
                        .keyPassphrase("heslo".toCharArray()))
                .build();

        assertThat("Private key should not be empty", conf.privateKey(), not(Optional.empty()));
        assertThat("Public key should not be empty", conf.publicKey(), not(Optional.empty()));
        assertThat("Public cert should not be empty", conf.publicCert(), not(Optional.empty()));

        conf.privateKey().ifPresent(it -> assertThat(it, instanceOf(RSAPrivateKey.class)));
        conf.publicKey().ifPresent(it -> assertThat(it, instanceOf(RSAPublicKey.class)));
        conf.publicCert().ifPresent(it -> assertThat(it, instanceOf(X509Certificate.class)));
        assertThat(conf.certChain().isEmpty(), is(false));
        assertThat(conf.certChain().size(), is(1));
        assertThat(conf.certChain().get(0), instanceOf(X509Certificate.class));
    }

    @Test
    void testPemConfig() {
        Keys conf = Keys.create(config.get("unit-11"));

        assertThat("Private key should not be empty", conf.privateKey(), not(Optional.empty()));
        assertThat("Public key should not be empty", conf.publicKey(), not(Optional.empty()));
        assertThat("Public cert should not be empty", conf.publicCert(), not(Optional.empty()));

        conf.privateKey().ifPresent(it -> assertThat(it, instanceOf(RSAPrivateKey.class)));
        conf.publicKey().ifPresent(it -> assertThat(it, instanceOf(RSAPublicKey.class)));
        conf.publicCert().ifPresent(it -> assertThat(it, instanceOf(X509Certificate.class)));
        assertThat(conf.certChain().isEmpty(), is(false));
        assertThat(conf.certChain().size(), is(1));
        assertThat(conf.certChain().get(0), instanceOf(X509Certificate.class));
    }

    @Test
    void testPemConfigNoPasswordNoChain() {
        Keys conf = Keys.create(config.get("unit-12"));

        assertThat("Private key should not be empty", conf.privateKey(), not(Optional.empty()));
        conf.privateKey().ifPresent(it -> assertThat(it, instanceOf(RSAPrivateKey.class)));

        assertThat(conf.publicKey(), is(Optional.empty()));
        assertThat(conf.publicCert(), is(Optional.empty()));
        assertThat("Cert chain must be empty", conf.certChain(), is(List.of()));
    }


    @Test
    void testTrustStoreCreationInMemoryWithoutPrivateKey() {
        Keys keystoreWithPrivateKey = Keys.create(config.get("unit-6-1"));

        Keys trustStore = Keys.builder()
                .keystore(keystore -> keystore
                        .trustStore(true)
                         // this two lines below are not neccessery
                         // currently KeystoreKeys.BuilderBase.validatePrototype requires keystore
                         // but with trustStore Resource shouldn't be required
                         .keystore(Resource.create("keystore/keystore.p12"))
                         .passphrase("password".toCharArray()))
		.addCerts(keystoreWithPrivateKey.certs())
                .build();

       assertThat(trustStore.privateKey(), is(Optional.empty()));
       assertThat(trustStore.publicKey(), is(Optional.empty()));
       assertThat(trustStore.certChain().size(), is(0)); // trustStore shouldn't contain private key and its certChain
       assertThat(trustStore.publicCert(), is(Optional.empty()));
       assertThat(trustStore.certs().size(), is(1));
    }

    @Test
    void testTrustStoreCreationFromKeystore() {

       Keys trustStore = Keys.builder()
                .keystore(keystore -> keystore
                       .trustStore(true)
                       .keystore(Resource.create("keystore/keystore.p12"))
                       .passphrase("password".toCharArray()))
               .build();

      assertThat(trustStore.privateKey(), is(Optional.empty()));
      assertThat(trustStore.publicKey(), is(Optional.empty()));
      assertThat(trustStore.certChain().size(), is(0)); // trustStore shouldn't contain private key and its certChain
      assertThat(trustStore.publicCert(), is(Optional.empty()));
      assertThat(trustStore.certs().size(), is(1));
   }


}
