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
package org.eclipse.che.workspace.infrastructure.docker.service;

import org.eclipse.che.api.core.jsonrpc.commons.RequestHandlerConfigurator;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.workspace.shared.dto.event.BootstrapperStatusEvent;
import org.eclipse.che.api.workspace.shared.dto.event.InstallerStatusEvent;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Service for handling bootstrapper & installer events.
 *
 * @author Max Shaposhnik (mshaposhnik@codenvy.com)
 */
@Singleton
public class InstallerJsonRpcService {
    private final EventService               eventService;

    @Inject
    public InstallerJsonRpcService(EventService eventService) {
        this.eventService = eventService;
    }

    @Inject
    public void configureMethods(RequestHandlerConfigurator requestHandler) {
        requestHandler.newConfiguration()
                      .methodName("bootstrapper/statusChanged")
                      .paramsAsDto(BootstrapperStatusEvent.class)
                      .noResult()
                      .withConsumer(this::handleBootstrapperStatus);

        requestHandler.newConfiguration()
                      .methodName("installer/statusChanged")
                      .paramsAsDto(InstallerStatusEvent.class)
                      .noResult()
                      .withConsumer(this::handleInstallerStatus);
    }

    public void handleInstallerStatus(InstallerStatusEvent installerStatusEvent) {
        eventService.publish(installerStatusEvent);
    }

    private void handleBootstrapperStatus(BootstrapperStatusEvent bootstrapperStatusEvent) {
        eventService.publish(bootstrapperStatusEvent);
    }
}
