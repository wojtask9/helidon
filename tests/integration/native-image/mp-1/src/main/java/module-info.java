/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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

/**
 * Application to test GraalVM native-image with Helidon MP.
 */
module helidon.tests.nimage.mp {
    requires jakarta.cdi;
    requires jakarta.ws.rs;
    requires io.helidon.security.annotations;
    requires io.helidon.security.abac.scope;
    requires jakarta.annotation;
    requires microprofile.openapi.api;
    requires jakarta.json;
    requires jakarta.inject;
    requires microprofile.jwt.auth.api;
    requires microprofile.health.api;
    requires io.helidon.security.jwt;
    requires io.helidon.microprofile.server;
    requires microprofile.fault.tolerance.api;
    requires microprofile.rest.client.api;
    requires microprofile.metrics.api;
    requires jakarta.json.bind;
    requires microprofile.config.api;
    // this is required, as otherwise the beans from this module
    // never reach health check CDI extension
    requires io.helidon.health.checks;
    requires io.opentracing.api;
    requires io.opentracing.util;

    exports io.helidon.tests.integration.nativeimage.mp1;
    exports io.helidon.tests.integration.nativeimage.mp1.other;

    // opens is needed to inject private fields, create classes in the same package (proxy)
    opens io.helidon.tests.integration.nativeimage.mp1;
    opens io.helidon.tests.integration.nativeimage.mp1.other;

    // we need to open the static resource on classpath directory to everybody, as otherwise
    // static content will not see it
    // if you want to add subdirectories, you will need to open them as well
    opens web;
}
