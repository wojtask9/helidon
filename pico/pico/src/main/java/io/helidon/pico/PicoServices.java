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

package io.helidon.pico;

import java.util.Map;
import java.util.Optional;

/**
 * Abstract factory for all services provided by a single Helidon Pico provider implementation.
 * An implementation of this interface must minimally supply a "services registry" - see {@link #services()}.
 * <p>
 * The global singleton instance is accessed via {@link #picoServices()}. Note that optionally one can provide a
 * primordial bootstrap configuration to the {@code Pico} services provider. One must establish any bootstrap instance
 * prior to the first call to {@link #picoServices()} as it will use a default configuration if not explicitly set. Once
 * the bootstrap has been set it cannot be changed for the lifespan of the JVM.
 */
public interface PicoServices {

    /**
     * Empty criteria will match anything and everything.
     */
    ServiceInfoCriteria EMPTY_CRITERIA = DefaultServiceInfoCriteria.builder().build();

    /**
     * Denotes a match to any (default) service, but required to be matched to at least one.
     */
    ContextualServiceQuery SERVICE_QUERY_REQUIRED = DefaultContextualServiceQuery.builder()
            .serviceInfo(EMPTY_CRITERIA)
            .expected(true)
            .build();

    /**
     * Returns the {@link io.helidon.pico.Bootstrap} configuration instance that was used to initialize this instance.
     *
     * @return the bootstrap configuration instance
     */
    Bootstrap bootstrap();

    /**
     * Retrieves any primordial bootstrap configuration that was already assigned via
     * {@link #globalBootstrap()} (Bootstrap)}.
     *
     * @return the bootstrap primordial configuration already assigned
     */
    static Optional<Bootstrap> globalBootstrap() {
        return PicoServicesHolder.bootstrap(false);
    }

    /**
     * Sets the primordial bootstrap configuration that will supply {@link #picoServices()} during global
     * singleton initialization.
     *
     * @param bootstrap the primordial global bootstrap configuration
     */
    static void globalBootstrap(Bootstrap bootstrap) {
        PicoServicesHolder.bootstrap(bootstrap);
    }

    /**
     * Get {@link PicoServices} instance if available. The highest {@link io.helidon.common.Weighted} service will be loaded
     * and returned. Remember to optionally configure any primordial {@link #bootstrap()} configuration prior to the
     * first call to get {@code PicoServices}.
     *
     * @return the Pico services instance
     */
    static Optional<PicoServices> picoServices() {
        return PicoServicesHolder.picoServices();
    }

    /**
     * The service registry.
     *
     * @return the services registry
     */
    Services services();

    /**
     * Creates a service binder instance for a specified module.
     *
     * @param module the module to offer binding to dynamically, and typically only at early startup initialization
     *
     * @return the service binder capable of binding, or empty if not permitted/available
     */
    default Optional<ServiceBinder> createServiceBinder(Module module) {
        return Optional.empty();
    }

    /**
     * Optionally, the injector.
     *
     * @return the injector, or empty if not available
     */
    default Optional<Injector> injector() {
        return Optional.empty();
    }

    /**
     * Optionally, the service providers' configuration.
     *
     * @return the config, or empty if not available
     */
    default Optional<PicoServicesConfig> config() {
        return Optional.empty();
    }

    /**
     * Attempts to perform a graceful {@link Injector#deactivate(Object, InjectorOptions)} on all managed
     * service instances in the {@link Services} registry.
     * Deactivation is handled within the current thread.
     * <p>
     * If the service provider does not support shutdown an empty is returned.
     * <p>
     * The default reference implementation for Pico will return a map of all service types that were deactivated to any
     * throwable that was observed during that services shutdown sequence.
     * <p>
     * The order in which services are deactivated is dependent upon whether the {@link #activationLog()} is available.
     * If the activation log is available, then services will be shutdown in reverse chronological order as how they
     * were started. If the activation log is not enabled or found to be empty then the deactivation will be in reverse
     * order of {@link RunLevel} from the highest value down to the lowest value. If two services share
     * the same {@link RunLevel} value then the ordering will be based upon the implementation's comparator.
     * <p>
     * When shutdown returns, it is guaranteed that all services were shutdown, or failed to shutdown.
     *
     * @return a map of all managed service types deactivated to results of deactivation
     */
    default Optional<Map<String, ActivationResult<?>>> shutdown() {
        return Optional.empty();
    }

    /**
     * Optionally, the service provider activation log.
     *
     * @return the injector, or empty if not available
     */
    default Optional<ActivationLog> activationLog() {
        return Optional.empty();
    }

}
