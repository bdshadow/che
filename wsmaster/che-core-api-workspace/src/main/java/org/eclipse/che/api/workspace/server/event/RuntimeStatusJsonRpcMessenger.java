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
package org.eclipse.che.api.workspace.server.event;

import org.eclipse.che.api.core.notification.RemoteEventService;
import org.eclipse.che.api.workspace.shared.dto.event.RuntimeStatusEvent;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

import static com.google.common.collect.Sets.newConcurrentHashSet;

/**
 * Send workspace events using JSON RPC to the clients
 */
@Singleton
public class RuntimeStatusJsonRpcMessenger {
    private final RemoteEventService remoteEventService;

    @Inject
    public RuntimeStatusJsonRpcMessenger(RemoteEventService remoteEventService) {
        this.remoteEventService = remoteEventService;
    }

    @PostConstruct
    private void postConstruct() {
        remoteEventService.register("runtime/statusChanged", RuntimeStatusEvent.class, this::predicate);
    }

    private boolean predicate(RuntimeStatusEvent event, Map<String, String> scope) {
        return event.getIdentity().getWorkspaceId().equals(scope.get("workspaceId"));
    }
}
