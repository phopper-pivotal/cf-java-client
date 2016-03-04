/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.reactor.util;


import org.cloudfoundry.Validatable;
import org.cloudfoundry.spring.client.v2.CloudFoundryExceptionBuilder;
import org.cloudfoundry.util.ExceptionUtils;
import org.cloudfoundry.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.core.tuple.Tuple;
import reactor.core.tuple.Tuple2;
import reactor.io.netty.common.NettyInbound;
import reactor.io.netty.http.HttpClient;
import reactor.io.netty.http.HttpException;
import reactor.io.netty.http.HttpInbound;
import reactor.io.netty.http.HttpOutbound;

import java.util.function.Consumer;

import static org.cloudfoundry.util.tuple.TupleUtils.function;

public abstract class AbstractReactorOperations {

    private final Logger logger = LoggerFactory.getLogger("cloudfoundry-client.request");

    private final AuthorizationProvider authorizationProvider;

    private final HttpClient httpClient;

    private final Mono<String> root;

    protected AbstractReactorOperations(AuthorizationProvider authorizationProvider, HttpClient httpClient, Mono<String> root) {
        this.authorizationProvider = authorizationProvider;
        this.httpClient = httpClient;
        this.root = root;
    }

    protected final <REQ extends Validatable, RSP> Mono<RSP> get(REQ request, Class<RSP> responseType, Consumer<Tuple2<UriComponentsBuilder, REQ>> builderCallback) {
        return Mono
            .when(ValidationUtils.validate(request), this.root)
            .map(function((validRequest, root) -> buildUri(root, validRequest, builderCallback)))
            .doOnSuccess(uri -> this.logger.debug("GET    {}", uri))
            .then(uri -> this.httpClient.get(uri,
                outbound -> this.authorizationProvider.addAuthorization(outbound)
                    .then(HttpOutbound::sendHeaders)))
            .otherwise(ExceptionUtils.replace(HttpException.class, CloudFoundryExceptionBuilder::build))
            .flatMap(NettyInbound::receiveInputStream)
            .as(JsonCodec.decode(responseType))
            .single();
    }

    protected final <REQ extends Validatable> Mono<HttpInbound> get(REQ request, Consumer<Tuple2<UriComponentsBuilder, REQ>> builderCallback) {
        return Mono
            .when(ValidationUtils.validate(request), this.root)
            .map(function((validRequest, root) -> buildUri(root, validRequest, builderCallback)))
            .doOnSuccess(uri -> this.logger.debug("GET    {}", uri))
            .then(uri -> this.httpClient.get(uri,
                outbound -> this.authorizationProvider.addAuthorization(outbound)
                    .then(HttpOutbound::sendHeaders)))
            .otherwise(ExceptionUtils.replace(HttpException.class, CloudFoundryExceptionBuilder::build));
    }

    protected final <REQ extends Validatable> Mono<HttpInbound> ws(REQ request, Consumer<Tuple2<UriComponentsBuilder, REQ>> builderCallback) {
        return Mono
            .when(ValidationUtils.validate(request), this.root)
            .map(function((validRequest, root) -> buildUri(root, validRequest, builderCallback)))
            .doOnSuccess(uri -> this.logger.debug("WS     {}", uri))
            .then(uri -> this.httpClient.get(uri,
                outbound -> this.authorizationProvider.addAuthorization(outbound)
                    .then(HttpOutbound::upgradeToWebsocket)))
            .otherwise(ExceptionUtils.replace(HttpException.class, CloudFoundryExceptionBuilder::build));
    }

    private static <REQ extends Validatable> String buildUri(String root, REQ validRequest, Consumer<Tuple2<UriComponentsBuilder, REQ>> builderCallback) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(root);
        builderCallback.accept(Tuple.of(builder, validRequest));
        return builder.build().encode().toUriString();
    }

}
