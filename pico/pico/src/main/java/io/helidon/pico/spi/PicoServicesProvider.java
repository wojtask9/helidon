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

package io.helidon.pico.spi;

import io.helidon.pico.Bootstrap;
import io.helidon.pico.PicoServices;

/**
 * Java {@link java.util.ServiceLoader} provider interface to find implementation of {@link io.helidon.pico.PicoServices}.
 */
public interface PicoServicesProvider {

    /**
     * Provide the {@code Pico} Services implementation, using the provided primordial {@link io.helidon.pico.Bootstrap}
     * configuration instance.
     *
     * @param bootstrap the primordial bootstrap configuration
     * @return Pico services
     */
    PicoServices services(Bootstrap bootstrap);

}
