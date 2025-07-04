/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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
 * HTTP/2 WebServer.
 */
@Feature(value = "HTTP/2",
         description = "HTTP/2 WebServer",
         in = HelidonFlavor.NIMA,
         invalidIn = HelidonFlavor.SE,
         path = {"WebServer", "HTTP/2"}
)
module io.helidon.nima.http2.webserver {
    requires static io.helidon.common.features.api;
    requires static io.helidon.config.metadata;

    requires transitive io.helidon.common;
    requires transitive io.helidon.common.uri;
    requires transitive io.helidon.common.socket;
    requires transitive io.helidon.common.task;
    requires transitive io.helidon.nima.webserver;
    requires transitive io.helidon.common.http;
    requires transitive io.helidon.nima.http2;
    requires transitive io.helidon.nima.http.encoding;
    requires transitive io.helidon.nima.http.media;
    requires io.helidon.pico.builder.config;
    requires io.helidon.builder;

    exports io.helidon.nima.http2.webserver;
    exports io.helidon.nima.http2.webserver.spi;

    // to support prior knowledge for h2c
    provides io.helidon.nima.webserver.spi.ServerConnectionProvider
            with io.helidon.nima.http2.webserver.Http2ConnectionProvider;
    // to support upgrade requests for h2c
    provides io.helidon.nima.webserver.http1.spi.Http1UpgradeProvider
            with io.helidon.nima.http2.webserver.Http2UpgradeProvider;

    uses io.helidon.nima.http2.webserver.spi.Http2SubProtocolProvider;
}
