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

package io.helidon.tracing.jaeger;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.metadata.Configured;
import io.helidon.config.metadata.ConfiguredOption;
import io.helidon.tracing.Tracer;
import io.helidon.tracing.TracerBuilder;
import io.helidon.tracing.opentelemetry.HelidonOpenTelemetry;
import io.helidon.tracing.opentelemetry.OpenTelemetryTracerProvider;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporterBuilder;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

/**
 * The JaegerTracerBuilder is a convenience builder for {@link io.helidon.tracing.Tracer} to use with Jaeger.
 * <p>
 * <b>Unless You want to explicitly depend on Jaeger in Your code, please
 * use {@link io.helidon.tracing.TracerBuilder#create(String)} or
 * {@link io.helidon.tracing.TracerBuilder#create(io.helidon.config.Config)} that is abstracted.</b>
 * <p>
 * The Jaeger tracer uses environment variables and system properties to override the defaults.
 * Except for {@code protocol} and {@code service} these are honored, unless overridden in configuration
 * or through the builder methods.
 * See <a href="https://github.com/jaegertracing/jaeger-client-java/blob/master/jaeger-core/README.md">Jaeger documentation</a>
 * for details.
 * <p>
 * The following table lists jaeger specific defaults and configuration options.
 * <table class="config">
 *     <caption>Tracer Configuration Options</caption>
 *     <tr>
 *         <th>option</th>
 *         <th>default</th>
 *         <th>description</th>
 *     </tr>
 *     <tr>
 *         <td>{@code service}</td>
 *         <td>&nbsp;</td>
 *         <td>Service name</td>
 *     </tr>
 *     <tr>
 *         <td>{@code protocol}</td>
 *         <td>{@code http}</td>
 *         <td>The protocol to use. By default http is used.</td>
 *     </tr>
 *     <tr>
 *         <td>{@code host}</td>
 *         <td>{@code 127.0.0.1}</td>
 *         <td>Host to use</td>
 *     </tr>
 *     <tr>
 *         <td>{@code port}</td>
 *         <td>{@value  #DEFAULT_HTTP_PORT}</td>
 *         <td>Port to be used</td>
 *     </tr>
 *     <tr>
 *         <td>{@code path}</td>
 *         <td>&nbsp;</td>
 *         <td>Path to be used.</td>
 *     </tr>
 *     <tr>
 *         <td>{@code exporter-timeout-millis}</td>
 *         <td>10 seconds</td>
 *         <td>Timeout of exporter</td>
 *     </tr>
 *     <tr>
 *         <td>{@code private-key-pem}</td>
 *         <td>&nbsp;</td>
 *         <td>Client private key in PEM format</td>
 *     </tr>
 *     <tr>
 *         <td>{@code client-cert-pem}</td>
 *         <td>&nbsp;</td>
 *         <td>Client certificate in PEM format</td>
 *     </tr>
 *     <tr>
 *         <td>{@code trusted-cert-pem}</td>
 *         <td>&nbsp;</td>
 *         <td>Trusted certificates in PEM format</td>
 *     </tr>
 *     <tr>
 *         <td>{@code sampler-type}</td>
 *         <td>{@code const} with param set to {@code 1}</td>
 *         <td>Sampler type {@code const} (0 to disable, 1 to always enabled),
 *              {@code ratio} (sample param contains the ratio as a double)</td>
 *     </tr>
 *     <tr>
 *         <td>{@code sampler-param}</td>
 *         <td>sampler type default</td>
 *         <td>Numeric parameter specifying details for the sampler type.</td>
 *     </tr>
 *     <tr>
 *         <td>{@code tags}</td>
 *         <td>&nbsp;</td>
 *         <td>see {@link io.helidon.tracing.TracerBuilder}</td>
 *     </tr>
 *     <tr>
 *         <td>{@code boolean-tags}</td>
 *         <td>&nbsp;</td>
 *         <td>see {@link io.helidon.tracing.TracerBuilder}</td>
 *     </tr>
 *     <tr>
 *         <td>{@code int-tags}</td>
 *         <td>&nbsp;</td>
 *         <td>see {@link io.helidon.tracing.TracerBuilder}</td>
 *     </tr>
 * </table>
 */
@Configured(prefix = "tracing", root = true, description = "Jaeger tracer configuration.")
public class JaegerTracerBuilder implements TracerBuilder<JaegerTracerBuilder> {
    static final System.Logger LOGGER = System.getLogger(JaegerTracerBuilder.class.getName());

    static final boolean DEFAULT_ENABLED = true;
    static final String DEFAULT_HTTP_HOST = "localhost";
    static final int DEFAULT_HTTP_PORT = 14250;

    private final Map<String, String> tags = new HashMap<>();
    private String serviceName;
    private String protocol = "http";
    private String host = DEFAULT_HTTP_HOST;
    private int port = DEFAULT_HTTP_PORT;
    private SamplerType samplerType = SamplerType.CONSTANT;
    private Number samplerParam = 1;
    private boolean enabled = DEFAULT_ENABLED;
    private boolean global = true;
    private Duration exporterTimeout = Duration.ofSeconds(10);
    private byte[] privateKey;
    private byte[] certificate;
    private byte[] trustedCertificates;
    private String path;

    /**
     * Default constructor, does not modify any state.
     */
    protected JaegerTracerBuilder() {
    }

    /**
     * Get a Jaeger {@link io.helidon.tracing.Tracer } builder for processing tracing data of a service with a given name.
     *
     * @param serviceName name of the service that will be using the tracer.
     * @return {@code Tracer} builder for Jaeger.
     */
    public static JaegerTracerBuilder forService(String serviceName) {
        return create()
                .serviceName(serviceName);
    }

    /**
     * Create a new builder based on values in configuration.
     * This requires at least a key "service" in the provided config.
     *
     * @param config configuration to load this builder from
     * @return a new builder instance.
     * @see io.helidon.tracing.jaeger.JaegerTracerBuilder#config(io.helidon.config.Config)
     */
    public static JaegerTracerBuilder create(Config config) {
        return create().config(config);
    }

    static JaegerTracerBuilder create() {
        return new JaegerTracerBuilder();
    }

    @Override
    public JaegerTracerBuilder serviceName(String name) {
        this.serviceName = name;
        return this;
    }

    @Override
    public JaegerTracerBuilder collectorProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    @Override
    public JaegerTracerBuilder collectorPort(int port) {
        this.port = port;
        return this;
    }

    @Override
    public JaegerTracerBuilder collectorPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public JaegerTracerBuilder collectorHost(String host) {
        this.host = host;
        return this;
    }

    @Override
    public JaegerTracerBuilder addTracerTag(String key, String value) {
        this.tags.put(key, value);
        return this;
    }

    @Override
    public JaegerTracerBuilder addTracerTag(String key, Number value) {
        this.tags.put(key, String.valueOf(value));
        return this;
    }

    @Override
    public JaegerTracerBuilder addTracerTag(String key, boolean value) {
        this.tags.put(key, String.valueOf(value));
        return this;
    }

    @Override
    public JaegerTracerBuilder config(Config config) {
        config.get("enabled").asBoolean().ifPresent(this::enabled);
        config.get("service").asString().ifPresent(this::serviceName);
        config.get("protocol").asString().ifPresent(this::collectorProtocol);
        config.get("host").asString().ifPresent(this::collectorHost);
        config.get("port").asInt().ifPresent(this::collectorPort);
        config.get("path").asString().ifPresent(this::collectorPath);
        config.get("sampler-type").asString().as(SamplerType::create).ifPresent(this::samplerType);
        config.get("sampler-param").asDouble().ifPresent(this::samplerParam);
        config.get("exporter-timeout-millis").asLong().ifPresent(it -> exporterTimeout(Duration.ofMillis(it)));
        config.get("private-key-pem").as(io.helidon.common.configurable.Resource::create).ifPresent(this::privateKey);
        config.get("client-cert-pem").as(io.helidon.common.configurable.Resource::create).ifPresent(this::clientCertificate);
        config.get("trusted-cert-pem").as(io.helidon.common.configurable.Resource::create).ifPresent(this::trustedCertificates);

        config.get("tags").detach()
                .asMap()
                .orElseGet(Map::of)
                .forEach(this::addTracerTag);

        config.get("boolean-tags")
                .asNodeList()
                .ifPresent(nodes -> {
                    nodes.forEach(node -> {
                        this.addTracerTag(node.key().name(), node.asBoolean().get());
                    });
                });

        config.get("int-tags")
                .asNodeList()
                .ifPresent(nodes -> {
                    nodes.forEach(node -> {
                        this.addTracerTag(node.key().name(), node.asInt().get());
                    });
                });

        config.get("global").asBoolean().ifPresent(this::registerGlobal);

        return this;
    }

    /**
     * Private key in PEM format.
     *
     * @param resource key resource
     * @return updated builder
     */
    @ConfiguredOption(key = "private-key-pem")
    public JaegerTracerBuilder privateKey(io.helidon.common.configurable.Resource resource) {
        this.privateKey = resource.bytes();
        return this;
    }

    /**
     * Certificate of client in PEM format.
     *
     * @param resource certificate resource
     * @return updated builder
     */
    @ConfiguredOption(key = "client-cert-pem")
    public JaegerTracerBuilder clientCertificate(io.helidon.common.configurable.Resource resource) {
        this.certificate = resource.bytes();
        return this;
    }

    /**
     * Trusted certificates in PEM format.
     *
     * @param resource trusted certificates resource
     * @return updated builder
     */
    @ConfiguredOption(key = "trusted-cert-pem")
    public JaegerTracerBuilder trustedCertificates(io.helidon.common.configurable.Resource resource) {
        this.trustedCertificates = resource.bytes();
        return this;
    }

    /**
     * Timeout of exporter requests.
     *
     * @param exporterTimeout timeout to use
     * @return updated builder
     */
    @ConfiguredOption(key = "exporter-timeout-millis", value = "10000")
    public JaegerTracerBuilder exporterTimeout(Duration exporterTimeout) {
        this.exporterTimeout = exporterTimeout;
        return this;
    }

    @Override
    public JaegerTracerBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    @Override
    public JaegerTracerBuilder registerGlobal(boolean global) {
        this.global = global;
        return this;
    }

    @Override
    public <B> B unwrap(Class<B> builderClass) {
        if (builderClass.isAssignableFrom(getClass())) {
            return builderClass.cast(this);
        }
        throw new IllegalArgumentException("This builder is a Jaeger tracer builder, cannot be unwrapped to "
                                                   + builderClass.getName());
    }

    /**
     * The sampler parameter (number).
     *
     * @param samplerParam parameter of the sampler
     * @return updated builder instance
     */
    @ConfiguredOption("1")
    public JaegerTracerBuilder samplerParam(Number samplerParam) {
        this.samplerParam = samplerParam;
        return this;
    }

    /**
     * Sampler type.
     * <p>
     * See <a href="https://www.jaegertracing.io/docs/latest/sampling/#client-sampling-configuration">Sampler types</a>.
     *
     * @param samplerType type of the sampler
     * @return updated builder instance
     */
    @ConfiguredOption("CONSTANT")
    public JaegerTracerBuilder samplerType(SamplerType samplerType) {
        this.samplerType = samplerType;
        return this;
    }

    /**
     * Builds the {@link io.helidon.tracing.Tracer} for Jaeger based on the configured parameters.
     *
     * @return the tracer
     */
    @Override
    public Tracer build() {
        Tracer result;

        if (enabled) {
            if (serviceName == null) {
                throw new IllegalArgumentException(
                        "Configuration must at least contain the 'service' key ('tracing.service` in MP) with service name");
            }

            JaegerGrpcSpanExporterBuilder spanExporterBuilder = JaegerGrpcSpanExporter.builder()
                    .setEndpoint(protocol + "://" + host + ":" + port + (path == null ? "" : path))
                    .setTimeout(exporterTimeout);

            if (privateKey != null && certificate != null) {
                spanExporterBuilder.setClientTls(privateKey, certificate);
            }

            if (trustedCertificates != null) {
                spanExporterBuilder.setTrustedCertificates(trustedCertificates);
            }

            SpanExporter exporter = spanExporterBuilder.build();

            Sampler sampler = switch (samplerType) {
                case RATIO -> Sampler.traceIdRatioBased(samplerParam.doubleValue());
                case CONSTANT -> samplerParam.intValue() == 1
                        ? Sampler.alwaysOn()
                        : Sampler.alwaysOff();
            };

            Resource serviceName = Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, this.serviceName));
            OpenTelemetry ot = OpenTelemetrySdk.builder()
                    .setTracerProvider(SdkTracerProvider.builder()
                                               .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                                               .setSampler(sampler)
                                               .setResource(serviceName)
                                               .build())
                    .setPropagators(ContextPropagators.create(B3Propagator.injectingMultiHeaders()))
                    .build();

            result = HelidonOpenTelemetry.create(ot, ot.getTracer(this.serviceName), tags);

            if (global) {
                GlobalOpenTelemetry.set(ot);
            }

            LOGGER.log(Level.INFO, () -> "Creating Jaeger tracer for '" + this.serviceName + "' configured with " + protocol
                    + "://" + host + ":" + port);
        } else {
            LOGGER.log(Level.INFO, "Jaeger Tracer is explicitly disabled.");
            result = Tracer.noOp();
        }

        if (global) {
            OpenTelemetryTracerProvider.globalTracer(result);
        }

        return result;
    }

    String path() {
        return path;
    }

    Map<String, String> tags() {
        return tags;
    }

    String serviceName() {
        return serviceName;
    }

    String protocol() {
        return protocol;
    }

    String host() {
        return host;
    }

    Integer port() {
        return port;
    }

    SamplerType samplerType() {
        return samplerType;
    }

    Number samplerParam() {
        return samplerParam;
    }

    boolean isEnabled() {
        return enabled;
    }

    /**
     * Sampler type definition.
     * Available options are "const", "probabilistic", "ratelimiting" and "remote".
     */
    public enum SamplerType {
        /**
         * Constant sampler always makes the same decision for all traces.
         * It either samples all traces {@code 1} or none of them {@code 0}.
         */
        CONSTANT("const"),
        /**
         * Ratio of the requests to sample, double value.
         */
        RATIO("ratio");
        private final String config;

        SamplerType(String config) {
            this.config = config;
        }

        static SamplerType create(String value) {
            for (SamplerType sampler : SamplerType.values()) {
                if (sampler.config().equals(value)) {
                    return sampler;
                }
            }
            throw new IllegalStateException("SamplerType " + value + " is not supported");
        }

        String config() {
            return config;
        }
    }
}
