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
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Defines a set of service methods for output
 * and corresponding set of handlers for these methods.
 *
 * @author Sergii Leshchenko
 * @author Anton Korneta
 */
@Singleton
public class OutputService {

    public static final String INSTALLER_LOG_METHOD_NAME = "installer/log";
    public static final String MACHINE_LOG_METHOD_NAME   = "machine/log";

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
                      .methodName(INSTALLER_LOG_METHOD_NAME)
                      .paramsAsDto(InstallerLogEvent.class)
                      .noResult()
                      .withConsumer(this::handleInstallerLog);

        requestHandler.newConfiguration()
                      .methodName(MACHINE_LOG_METHOD_NAME)
                      .paramsAsDto(MachineLogEvent.class)
                      .noResult()
                      .withConsumer(this::handleMachineLog);
    }

    private void handleInstallerLog(InstallerLogEvent installerStatusEvent) {
        remoteEventService.publish(INSTALLER_LOG_METHOD_NAME,
                                   installerStatusEvent,
                                   (event, scope) -> event.getRuntimeId()
                                                          .getWorkspaceId()
                                                          .equals(scope.get("workspaceId")));
    }

    private void handleMachineLog(MachineLogEvent installerStatusEvent) {
        remoteEventService.publish(MACHINE_LOG_METHOD_NAME,
                                   installerStatusEvent,
                                   (event, scope) -> event.getRuntimeId()
                                                          .getWorkspaceId()
                                                          .equals(scope.get("workspaceId")));
    }
}
