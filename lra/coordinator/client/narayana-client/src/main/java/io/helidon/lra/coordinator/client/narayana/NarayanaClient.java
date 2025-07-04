/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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
package io.helidon.lra.coordinator.client.narayana;

import java.lang.System.Logger.Level;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.helidon.common.http.Headers;
import io.helidon.common.http.Http;
import io.helidon.common.reactive.Single;
import io.helidon.lra.coordinator.client.CoordinatorClient;
import io.helidon.lra.coordinator.client.CoordinatorConnectionException;
import io.helidon.lra.coordinator.client.Participant;
import io.helidon.lra.coordinator.client.PropagatedHeaders;
import io.helidon.reactive.faulttolerance.Retry;
import io.helidon.reactive.webclient.WebClient;
import io.helidon.reactive.webclient.WebClientRequestHeaders;
import io.helidon.reactive.webclient.WebClientResponse;

import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

/**
 * Narayana LRA coordinator client.
 */
public class NarayanaClient implements CoordinatorClient {
    private static final Http.HeaderName LRA_HTTP_CONTEXT_HEADER = Http.Header.create(LRA.LRA_HTTP_CONTEXT_HEADER);
    private static final Http.HeaderName LRA_HTTP_RECOVERY_HEADER = Http.Header.create(LRA.LRA_HTTP_RECOVERY_HEADER);

    private static final System.Logger LOGGER = System.getLogger(NarayanaClient.class.getName());

    private static final int RETRY_ATTEMPTS = 5;
    private static final String QUERY_PARAM_CLIENT_ID = "ClientID";
    private static final String QUERY_PARAM_TIME_LIMIT = "TimeLimit";
    private static final String QUERY_PARAM_PARENT_LRA = "ParentLRA";
    private static final String HEADER_LINK = "Link";
    private static final Pattern LRA_ID_PATTERN = Pattern.compile("(.*)/([^/?]+).*");

    private Supplier<URI> coordinatorUriSupplier;
    private Duration coordinatorTimeout;
    private Retry retry;

    @Override
    public void init(Supplier<URI> coordinatorUriSupplier, Duration timeout) {
        this.coordinatorUriSupplier = coordinatorUriSupplier;
        this.coordinatorTimeout = timeout;
        this.retry = Retry.builder()
                .overallTimeout(timeout)
                .retryPolicy(Retry.JitterRetryPolicy.builder()
                        .calls(RETRY_ATTEMPTS)
                        .build())
                .build();
    }

    @Override
    public Single<URI> start(String clientID, PropagatedHeaders headers, long timeout) {
        return startInternal(null, clientID, headers, timeout);
    }

    @Override
    public Single<URI> start(URI parentLRAUri, String clientID, PropagatedHeaders headers, long timeout) {
        return startInternal(parentLRAUri, clientID, headers, timeout);
    }

    private Single<URI> startInternal(URI parentLRA, String clientID, PropagatedHeaders headers, long timeout) {
        // We need to call coordinator which knows parent LRA
        URI baseUri = Optional.ofNullable(parentLRA)
                .map(p -> parseBaseUri(p.toASCIIString()))
                .orElse(coordinatorUriSupplier.get());

        return retry.invoke(() -> prepareWebClient(baseUri)
                .post()
                .path("start")
                .headers(copyHeaders(headers)) // header propagation
                .queryParam(QUERY_PARAM_CLIENT_ID, Optional.ofNullable(clientID).orElse(""))
                .queryParam(QUERY_PARAM_TIME_LIMIT, String.valueOf(timeout))
                .queryParam(QUERY_PARAM_PARENT_LRA, parentLRA == null ? "" : parentLRA.toASCIIString())
                .submit()
                .flatMap(res -> {
                    Http.Status status = res.status();
                    if (status.code() != 201) {
                        return res.content().as(String.class).flatMap(cont ->
                                connectionError("Unexpected response " + status + " from coordinator "
                                        + res.lastEndpointURI() + ": " + cont, null));
                    } else {
                        //propagate supported headers from coordinator
                        headers.scan(res.headers().toMap());
                        return Single.just(res);
                    }
                })
                .map(res -> res
                        .headers()
                        .location()
                        // TMM doesn't send lraId as LOCATION
                        .or(() -> res.headers()
                                .first(LRA_HTTP_CONTEXT_HEADER)
                                .map(URI::create))
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Coordinator needs to return lraId either as 'Location' or "
                                                + "'Long-Running-Action' header."))
                )
                .onErrorResumeWith(t -> connectionError("Unable to start LRA", t))
                .peek(lraId -> logF("LRA started - LRAID: {0} parent: {1}", lraId, parentLRA))
                .first());
    }

    @Override
    public Single<Void> cancel(URI lraId, PropagatedHeaders headers) {
        return retry.invoke(() -> prepareWebClient(lraId)
                .put()
                .path("/cancel")
                .headers(copyHeaders(headers)) // header propagation
                .submit()
                .map(WebClientResponse::status)
                .flatMap(status -> {
                    switch (status.family()) {
                        case SUCCESSFUL:
                            logF("LRA cancelled - LRAID: {0}", lraId);
                            return Single.empty();
                        case CLIENT_ERROR:
                        default:
                            if (404 == status.code()) {
                                LOGGER.log(Level.WARNING, "Cancel LRA - Coordinator can't find id - LRAID: " + lraId);
                                return Single.empty();
                            }
                            return connectionError("Unable to cancel lra " + lraId, status.code());
                    }
                })
                .onErrorResumeWith(t -> connectionError("Unable to cancel LRA " + lraId, t))
                .first()
                .map(Void.TYPE::cast)
        );
    }

    @Override
    public Single<Void> close(URI lraId, PropagatedHeaders headers) {
        return retry.invoke(() -> prepareWebClient(lraId)
                .put()
                .path("/close")
                .headers(copyHeaders(headers)) // header propagation
                .submit()
                .map(WebClientResponse::status)
                .flatMap(status -> {
                    switch (status.family()) {
                        case SUCCESSFUL:
                            logF("LRA closed - LRAID: {0}", lraId);
                            return Single.empty();
                        case CLIENT_ERROR:
                        default:
                            // 404 can happen when coordinator already cleaned terminated lra's
                            if (List.of(410, 404).contains(status.code())) {
                                logF("LRA already closed - LRAID: {0}", lraId);
                                return Single.empty();
                            }
                            return connectionError("Unable to close lra - LRAID: " + lraId, status.code());
                    }
                })
                .onErrorResumeWith(t -> connectionError("Unable to close LRA " + lraId, t))
                .first()
                .map(Void.TYPE::cast)
        );
    }

    @Override
    public Single<Optional<URI>> join(URI lraId,
                                      PropagatedHeaders headers,
                                      long timeLimit,
                                      Participant p) {
        String links = compensatorLinks(p);

        return retry.invoke(() -> prepareWebClient(lraId)
                .put()
                .queryParam(QUERY_PARAM_TIME_LIMIT, String.valueOf(timeLimit))
                .headers(h -> {
                    h.add(HEADER_LINK, links); // links are expected either in header
                    headers.toMap().forEach((name, value) -> h.set(Http.Header.create(name), value)); // header propagation
                    return h;
                })
                .submit(links) // or as a body
                .flatMap(res -> {
                    switch (res.status().code()) {
                        case 412:
                            return connectionError(res.lastEndpointURI()
                                    + " Too late to join LRA - LRAID: " + lraId, 412);
                        case 404:
                            // Narayana returns 404 for already terminated lras
                        case 410:
                            return connectionError("Not found " + lraId, 410);
                        case 200:
                            return Single.just(res
                                    .headers()
                                    .first(LRA_HTTP_RECOVERY_HEADER)
                                    .map(URI::create));

                        default:
                            return connectionError("Unexpected coordinator response ", res.status().code());
                    }
                })
                .onErrorResumeWith(t -> connectionError("Unable to join LRA", t))
                .peek(uri -> logF("Participant {0} joined - LRAID: {1}", p, lraId))
                .first());
    }

    @Override
    public Single<Void> leave(URI lraId, PropagatedHeaders headers, Participant p) {
        return retry.invoke(() -> prepareWebClient(lraId)
                .put()
                .path("/remove")
                .headers(copyHeaders(headers)) // header propagation
                .submit(compensatorLinks(p))
                .flatMap(res -> {
                    switch (res.status().code()) {
                        case 404:
                            LOGGER.log(Level.WARNING,
                                    "Participant {0} leaving LRA - Coordinator can't find id - LRAID: {1}",
                                    new Object[] {p, lraId});
                            return Single.empty();
                        case 200:
                            logF("Participant {0} left - LRAID: {1}", p, lraId);
                            return Single.empty();
                        default:
                            throw new IllegalStateException("Unexpected coordinator response " + res.status());
                    }
                })
                .onErrorResumeWith(t -> connectionError("Unable to leave LRA " + lraId, t))
                .first()
                .map(Void.TYPE::cast)
        );
    }


    @Override
    public Single<LRAStatus> status(URI lraId, PropagatedHeaders headers) {
        return retry.invoke(() -> prepareWebClient(lraId)
                .get()
                .path("/status")
                .headers(copyHeaders(headers)) // header propagation
                .request()
                .flatMap(res -> {
                    switch (res.status().code()) {
                        case 404:
                            LOGGER.log(Level.WARNING, "Status LRA - Coordinator can't find id - LRAID: " + lraId);
                            return Single.just(LRAStatus.Closed);
                        case 200:
                        case 202:
                            return res
                                    .content()
                                    .as(LRAStatus.class)
                                    .peek(status -> logF("LRA status {0} retrieved - LRAID: {1}", status, lraId));
                        default:
                            throw new IllegalStateException("Unexpected coordinator response " + res.status());
                    }
                })
                .onErrorResumeWith(t ->
                        connectionError("Unable to retrieve LRA status of " + lraId, t))
                .first()
        );
    }

    private WebClient prepareWebClient(URI uri) {
        return WebClient.builder()
                .baseUri(uri)
                // Workaround for #3242
                .keepAlive(false)
                .connectTimeout(coordinatorTimeout)
                .readTimeout(coordinatorTimeout)
                .addReader(new LraStatusReader())
                .build();
    }

    /**
     * Narayana accepts participant's links as RFC 5988 {@code jakarta.ws.rs.core.Link}s delimited by commas.
     * <p>
     * Example:
     * <pre>{@code
     * <http://127.0.0.1:8080/lraresource/status>; rel="status"; title="status URI"; type="text/plain",
     * <http://127.0.0.1:8080/lraresource/compensate>; rel="compensate"; title="compensate URI"; type="text/plain",
     * <http://127.0.0.1:8080/lraresource/after>; rel="after"; title="after URI"; type="text/plain",
     * <http://127.0.0.1:8080/lraresource/complete>; rel="complete"; title="complete URI"; type="text/plain",
     * <http://127.0.0.1:8080/lraresource/forget>; rel="forget"; title="forget URI"; type="text/plain",
     * <http://127.0.0.1:8080/lraresource/leave>; rel="leave"; title="leave URI"; type="text/plain"
     * }</pre>
     *
     * @param p participant to serialize as links
     * @return links delimited by comma
     */
    private String compensatorLinks(Participant p) {
        return Map.of(
                "compensate", p.compensate(),
                "complete", p.complete(),
                "forget", p.forget(),
                "leave", p.leave(),
                "after", p.after(),
                "status", p.status()
        )
                .entrySet()
                .stream()
                .filter(e -> e.getValue().isPresent())
                // rfc 5988
                .map(e -> String.format("<%s>; rel=\"%s\"; title=\"%s\"; type=\"text/plain\"",
                        e.getValue().get(),
                        e.getKey(),
                        e.getKey() + " URI"))
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    static URI parseBaseUri(String lraUri) {
        Matcher m = LRA_ID_PATTERN.matcher(lraUri);
        if (!m.matches()) {
            //LRA id uri format
            throw new RuntimeException("Error when parsing lra uri: " + lraUri);
        }
        return URI.create(m.group(1));
    }

    private Function<WebClientRequestHeaders, Headers> copyHeaders(PropagatedHeaders headers) {
        return wcHeaders -> {
            headers.toMap().forEach((key, value) -> wcHeaders.set(Http.Header.create(key), value));
            return wcHeaders;
        };
    }

    private <T> Single<T> connectionError(String message, int status) {
        LOGGER.log(Level.WARNING, message);
        return Single.error(new CoordinatorConnectionException(message, status));
    }

    private <T> Single<T> connectionError(String message, Throwable cause) {
        LOGGER.log(Level.WARNING, message, cause);
        if (cause instanceof CoordinatorConnectionException) {
            return Single.error(cause);
        }
        return Single.error(new CoordinatorConnectionException(message, cause, 500));
    }

    private void logF(String msg, Object... params) {
        LOGGER.log(Level.DEBUG, msg, params);
    }
}

