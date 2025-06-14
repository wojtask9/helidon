/*
 * Copyright (c) 2019, 2023 Oracle and/or its affiliates.
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

package io.helidon.grpc.metrics;

import java.lang.System.Logger.Level;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.helidon.common.http.HttpMediaType;
import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcServer;
import io.helidon.grpc.server.GrpcServerConfiguration;
import io.helidon.grpc.server.test.Echo;
import io.helidon.grpc.server.test.EchoServiceGrpc;
import io.helidon.logging.common.LogConfig;
import io.helidon.reactive.media.jsonp.JsonpSupport;
import io.helidon.reactive.metrics.MetricsSupport;
import io.helidon.reactive.webclient.WebClient;
import io.helidon.reactive.webserver.Routing;
import io.helidon.reactive.webserver.WebServer;

import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import services.EchoService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration tests for gRPC server with metrics.
 */
@Disabled
public class MetricsIT {

    // ----- data members ---------------------------------------------------

    /**
     * The Helidon {@link WebServer} to use for testing.
     */
    private static WebServer webServer;

    /**
     * The Helidon {@link WebClient} to use to make http requests to the {@link WebServer}.
     */
    private static WebClient client;

    /**
     * The {@link System.Logger} to use for logging.
     */
    private static final System.Logger LOGGER = System.getLogger(MetricsIT.class.getName());

    /**
     * The Helidon {@link io.helidon.grpc.server.GrpcServer} being tested.
     */
    private static GrpcServer grpcServer;

    /**
     * A gRPC {@link Channel} to connect to the test gRPC server
     */
    private static Channel channel;

    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    public static void setup() throws Exception {
        LogConfig.configureRuntime();

        // start the server at a free port
        startWebServer();


        client = WebClient.builder()
                .followRedirects(true)
                .addMediaSupport(JsonpSupport.create())
                .build();

        startGrpcServer();

        channel = ManagedChannelBuilder.forAddress("localhost", grpcServer.port())
                                       .usePlaintext()
                                       .build();
    }

    @AfterAll
    public static void cleanup() throws Exception {
        if (webServer != null) {
            webServer.shutdown()
                     .toCompletableFuture()
                     .get(10, TimeUnit.SECONDS);
        }
    }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldPublishMetrics() throws ExecutionException, InterruptedException {
        // call the gRPC Echo service so that there should be some metrics
        EchoServiceGrpc.newBlockingStub(channel).echo(Echo.EchoRequest.newBuilder().setMessage("foo").build());

        // request the application metrics in json format from the web server
        client.get()
                .uri("http://localhost:" + webServer.port())
                .path("metrics/application")
                .accept(HttpMediaType.APPLICATION_JSON)
                .request(JsonStructure.class)
                .thenAccept(it -> {
                    JsonValue value = it.getValue("/EchoService.Echo");
                    assertThat(value, is(notNullValue()));
                })
                .toCompletableFuture()
                .get();
    }

    // ----- helper methods -------------------------------------------------

    /**
     * Start the gRPC Server listening on an ephemeral port.
     *
     * @throws Exception in case of an error
     */
    private static void startGrpcServer() throws Exception {
        // Add the EchoService and enable GrpcMetrics
        GrpcRouting routing = GrpcRouting.builder()
                                         .intercept(GrpcMetrics.timed())
                                         .register(new EchoService(), rules -> rules.intercept(GrpcMetrics.metered())
                                                                                    .intercept("Echo",
                                                                                               GrpcMetrics.counted()))
                                         .build();

        // Run the server on port 0 so that it picks a free ephemeral port
        GrpcServerConfiguration serverConfig = GrpcServerConfiguration.builder().port(0).build();

        grpcServer = GrpcServer.create(serverConfig, routing)
                        .start()
                        .toCompletableFuture()
                        .get(10, TimeUnit.SECONDS);

       LOGGER.log(Level.INFO, "Started gRPC server at: localhost:" + grpcServer.port());
    }

    /**
     * Start the Web Server listening on an ephemeral port.
     *
     * @throws Exception in case of an error
     */
    private static void startWebServer() throws Exception {
        // Add metrics to the web server routing
        Routing routing = Routing.builder()
                .register(MetricsSupport.create())
                .build();

        // Web server picks a free ephemeral port by default
        webServer = WebServer.create(routing)
                     .start()
                     .toCompletableFuture()
                     .get(10, TimeUnit.SECONDS);

        LOGGER.log(Level.INFO, "Started web server at: http://localhost:" + webServer.port());
    }
}
