/*
 * Copyright (c) 2018, 2023 Oracle and/or its affiliates.
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

import io.helidon.common.features.api.Feature;
import io.helidon.common.features.api.HelidonFlavor;

/**
 * Zipkin tracing support.
 */
@Feature(value = "Zipkin",
        description = "Zipkin tracer integration",
        in = {HelidonFlavor.MP, HelidonFlavor.SE, HelidonFlavor.NIMA},
        path = {"Tracing", "Zipkin"}
)
module io.helidon.tracing.zipkin {
    requires static io.helidon.common.features.api;

    requires io.helidon.common;
    requires io.helidon.config;
    requires io.helidon.tracing;
    requires io.helidon.tracing.opentracing;
    requires static io.helidon.config.metadata;

    requires io.opentracing.util;
    requires brave.opentracing;
    requires zipkin2.reporter;
    requires zipkin2.reporter.urlconnection;
    requires zipkin2;
    requires brave;
    requires io.opentracing.noop;
    requires io.opentracing.api;

    exports io.helidon.tracing.zipkin;

    provides io.helidon.tracing.opentracing.spi.OpenTracingProvider with io.helidon.tracing.zipkin.ZipkinTracerProvider;
}
