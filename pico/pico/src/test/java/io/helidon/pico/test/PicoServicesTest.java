/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.pico.test;

import io.helidon.pico.Bootstrap;
import io.helidon.pico.DefaultBootstrap;
import io.helidon.pico.PicoServices;
import io.helidon.pico.test.testsubjects.PicoServices2;

import org.junit.jupiter.api.Test;

import static io.helidon.common.testing.junit5.OptionalMatcher.optionalEmpty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PicoServices tests.
 */
class PicoServicesTest {

    /**
     * Test basic loader.
     */
    @Test
    void testGetPicoServices() {
        assertThat(PicoServices.globalBootstrap(), optionalEmpty());
        Bootstrap bootstrap = DefaultBootstrap.builder().build();
        PicoServices.globalBootstrap(bootstrap);

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> PicoServices.globalBootstrap(bootstrap));
        assertThat(e.getMessage(), equalTo("bootstrap already set"));

        PicoServices picoServices = PicoServices.picoServices().get();
        assertThat(picoServices, notNullValue());
        assertThat(picoServices, instanceOf(PicoServices2.class));
        assertThat(picoServices, sameInstance(PicoServices.picoServices().get()));

        assertThat(picoServices.bootstrap(), sameInstance(bootstrap));
    }

}
