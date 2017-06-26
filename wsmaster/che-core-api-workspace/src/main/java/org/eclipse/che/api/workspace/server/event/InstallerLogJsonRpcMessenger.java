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
import org.eclipse.che.api.workspace.shared.dto.event.InstallerLogEvent;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Send infrastructure log events using JSON-RPC to the clients.
 *
 * @author Anton Korneta
 */
@Singleton
public class InstallerLogJsonRpcMessenger {
    private final RemoteEventService remoteEventService;

    @Inject
    public InstallerLogJsonRpcMessenger(RemoteEventService remoteEventService) {
        this.remoteEventService = remoteEventService;
    }

    @PostConstruct
    private void postConstruct() {
        remoteEventService.register("installer/log", InstallerLogEvent.class, this::predicate);
    }

    private boolean predicate(InstallerLogEvent event, Map<String, String> scope) {
        return event.getRuntimeId().getWorkspaceId().equals(scope.get("workspaceId"));
    }

}
