/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.core.notification;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.core.jsonrpc.commons.RequestHandlerConfigurator;
import org.eclipse.che.api.core.jsonrpc.commons.RequestTransmitter;
import org.eclipse.che.api.core.notification.dto.EventSubscription;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

import static java.util.Collections.emptySet;

/**
 * Dispatches events to subscribed listeners via JSON-RPC.
 *
 * <p>Publishing of events can be performed:
 * <ul>
 * <li>manually by {@link #publish(String, Object, BiPredicate) publish} method;</li>
 * <li>automatically from {@link EventService} if corresponding event is
 * registered to republish by {@link #register(String, Class, BiPredicate) register} method.</li>
 * </ul>
 *
 * <p>For starting receiving events from remote event service client must
 * subscribe on required events by sending following message:
 * <pre>
 *   {
 *     "jsonrpc":"2.0",
 *     "method":"subscribe",
 *     "params": {
 *       "method":"demo/method",
 *         "scope": {
 *           "channelId":"channel123"
 *         }
 *       }
 *   }
 * </pre>
 *
 * <p>For stopping receiving events from remote event service client must send following message:
 * <pre>
 *   {
 *     "jsonrpc":"2.0",
 *     "method":"unSubscribe",
 *     "params": {
 *       "method":"demo/method",
 *         "scope": {
 *           "channelId":"channel123"
 *         }
 *       }
 *   }
 * </pre>
 */
@Singleton
public class RemoteEventService {
    private final Map<String, Set<SubscriptionContext>> subscriptionContexts = new ConcurrentHashMap<>();

    private final EventService       eventService;
    private final RequestTransmitter requestTransmitter;

    @Inject
    public RemoteEventService(EventService eventService, RequestTransmitter requestTransmitter) {
        this.eventService = eventService;
        this.requestTransmitter = requestTransmitter;
    }

    @Inject
    private void registerMethods(RequestHandlerConfigurator requestHandlerConfigurator) {
        requestHandlerConfigurator.newConfiguration()
                                  .methodName("subscribe")
                                  .paramsAsDto(EventSubscription.class)
                                  .noResult()
                                  .withBiConsumer(this::consumeSubscriptionRequest);

        requestHandlerConfigurator.newConfiguration()
                                  .methodName("unSubscribe")
                                  .paramsAsDto(EventSubscription.class)
                                  .noResult()
                                  .withBiConsumer(this::consumeUnSubscriptionRequest);
    }

    public <T> void register(String method, Class<T> eventType, BiPredicate<T, Map<String, String>> biPredicate) {
        eventService.subscribe(event -> subscriptionContexts.getOrDefault(method, new HashSet<>())
                                                            .stream()
                                                            .filter(context -> biPredicate.test(event, context.scope))
                                                            .forEach(context -> transmit(context.endpointId, method,
                                                                                         event)),
                               eventType);
    }

    /**
     * Publishes events to subscribed clients.
     *
     * @param method
     *         method to which will be published event
     * @param event
     *         event to publish
     * @param predicate
     *         predicate for filtering clients
     */
    public <T> void publish(String method, T event, BiPredicate<T, Map<String, String>> predicate) {
        subscriptionContexts.getOrDefault(method, emptySet())
                            .stream()
                            .filter(context -> predicate.test(event, context.scope))
                            .forEach(context -> transmit(context.endpointId, method, event));
    }

    private void consumeSubscriptionRequest(String endpointId, EventSubscription eventSubscription) {
        subscriptionContexts.computeIfAbsent(eventSubscription.getMethod(), k -> new HashSet<>())
                            .add(new SubscriptionContext(endpointId, eventSubscription.getScope()));
    }

    private void consumeUnSubscriptionRequest(String endpointId, EventSubscription eventSubscription) {
        subscriptionContexts.getOrDefault(eventSubscription.getMethod(), emptySet())
                            .removeIf(
                                    subscriptionContext -> Objects.equals(subscriptionContext.endpointId, endpointId));
    }

    private <T> void transmit(String endpointId, String method, T event) {
        requestTransmitter.newRequest()
                          .endpointId(endpointId)
                          .methodName(method)
                          .paramsAsDto(event)
                          .sendAndSkipResult();
    }

    private class SubscriptionContext {
        private final String              endpointId;
        private final Map<String, String> scope;

        private SubscriptionContext(String endpointId, Map<String, String> scope) {
            this.endpointId = endpointId;
            this.scope = scope;
        }
    }
}
