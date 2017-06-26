package org.eclipse.che.workspace.infrastructure.docker;

import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.core.notification.RemoteEventService;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.workspace.server.DtoConverter;
import org.eclipse.che.api.workspace.shared.dto.event.MachineLogEvent;
import org.eclipse.che.dto.server.DtoFactory;

import java.io.IOException;

/**
 * Consumes machine logs text line and publishes it in form of log event.
 *
 * @author Anton Korneta
 */
public class MachineLogsLineConsumer implements LineConsumer {

    private final RemoteEventService remoteEventService;
    private final MachineLogEvent    logEvent;

    public MachineLogsLineConsumer(RemoteEventService remoteEventService,
                                   RuntimeIdentity runtime,
                                   String machineName) {
        this.remoteEventService = remoteEventService;
        this.logEvent = DtoFactory.newDto(MachineLogEvent.class)
                                  .withRuntimeId(DtoConverter.asDto(runtime))
                                  .withMachineName(machineName);
    }

    @Override
    public void writeLine(String line) throws IOException {
        logEvent.withText(line);
        remoteEventService.publish("machine/log",
                                   logEvent,
                                   (event, scope) -> event.getRuntimeId()
                                                          .getWorkspaceId()
                                                          .equals(scope.get("workspaceId")));
    }

    @Override
    public void close() throws IOException {}

}
