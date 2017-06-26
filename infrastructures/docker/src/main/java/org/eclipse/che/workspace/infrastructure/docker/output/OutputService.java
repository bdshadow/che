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
package org.eclipse.che.workspace.infrastructure.docker.output;

import org.eclipse.che.api.core.jsonrpc.commons.RequestHandlerConfigurator;
import org.eclipse.che.api.core.notification.RemoteEventService;
import org.eclipse.che.api.workspace.shared.dto.event.InstallerLogEvent;
import org.eclipse.che.api.workspace.shared.dto.event.MachineLogEvent;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service for handling output.
 *
 * @author Sergii Leshchenko
 */
@Singleton
public class OutputService {
    private final RequestHandlerConfigurator requestHandler;
    private final RemoteEventService         remoteEventService;

    @Inject
    public OutputService(RequestHandlerConfigurator requestHandler,
                         RemoteEventService manager) {
        this.requestHandler = requestHandler;
        this.remoteEventService = manager;
    }

    @PostConstruct
    public void configureMethods() {
        requestHandler.newConfiguration()
                      .methodName("installer/log")
                      .paramsAsDto(InstallerLogEvent.class)
                      .noResult()
                      .withConsumer(this::handleInstallerLog);

        requestHandler.newConfiguration()
                      .methodName("machine/log")
                      .paramsAsDto(MachineLogEvent.class)
                      .noResult()
                      .withConsumer(this::handleMachineLog);
    }

    private void handleInstallerLog(InstallerLogEvent installerStatusEvent) {
        remoteEventService.publish("installer/log",
                                   installerStatusEvent,
                                   (event, scope) -> event.getRuntimeId()
                                                          .getWorkspaceId()
                                                          .equals(scope.get("workspaceId")));
    }

    private void handleMachineLog(MachineLogEvent installerStatusEvent) {
        remoteEventService.publish("machine/log",
                                   installerStatusEvent,
                                   (event, scope) -> event.getRuntimeId()
                                                          .getWorkspaceId()
                                                          .equals(scope.get("workspaceId")));
    }
}
