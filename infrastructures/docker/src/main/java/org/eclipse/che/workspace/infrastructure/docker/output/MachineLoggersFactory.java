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

import org.eclipse.che.api.core.model.workspace.runtime.RuntimeIdentity;
import org.eclipse.che.api.core.notification.RemoteEventService;
import org.eclipse.che.api.workspace.server.DtoConverter;
import org.eclipse.che.api.workspace.shared.dto.event.MachineLogEvent;
import org.eclipse.che.dto.server.DtoFactory;
import org.eclipse.che.plugin.docker.client.LogMessage;
import org.eclipse.che.plugin.docker.client.MessageProcessor;
import org.eclipse.che.plugin.docker.client.ProgressLineFormatterImpl;
import org.eclipse.che.plugin.docker.client.ProgressMonitor;
import org.eclipse.che.plugin.docker.client.json.ProgressStatus;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.ZonedDateTime;
import java.util.function.BiConsumer;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.eclipse.che.plugin.docker.client.LogMessage.Type.DOCKER;
import static org.eclipse.che.workspace.infrastructure.docker.output.OutputService.MACHINE_LOG_METHOD_NAME;

/**
 * Produces machine logs publishers.
 *
 * @author Anton Korneta
 */
@Singleton
public class MachineLoggersFactory {

    private final RemoteEventService remoteEventService;

    @Inject
    public MachineLoggersFactory(RemoteEventService remoteEventService) {
        this.remoteEventService = remoteEventService;
    }

    /**
     * Produces new instance of machine progress monitor.
     *
     * @param machineName
     *         name of machine
     * @param runtime
     *         runtime identity for given machine
     */
    public ProgressMonitor newProgressMonitor(String machineName, RuntimeIdentity runtime) {
        return new MachineProgressMonitor(new MachineLogsBiConsumer(machineName, runtime));
    }

    /**
     * Produces new instance of {@link MachineLogMessageProcessor}.
     *
     * @param machineName
     *         name of machine
     * @param runtime
     *         runtime identity for given machine
     */
    public MessageProcessor<LogMessage> newLogsProcessor(String machineName, RuntimeIdentity runtime) {
        return new MachineLogMessageProcessor(new MachineLogsBiConsumer(machineName, runtime));
    }

    /**
     * Supplies text and stream to {@link MachineLogsBiConsumer} from {@link LogMessage}.
     */
    private class MachineLogMessageProcessor implements MessageProcessor<LogMessage> {

        private final BiConsumer<String, String> biConsumer;

        public MachineLogMessageProcessor(BiConsumer<String, String> biConsumer) {
            this.biConsumer = biConsumer;
        }

        @Override
        public void process(LogMessage message) {
            final LogMessage.Type type = message.getType();
            if (type == DOCKER) {
                biConsumer.accept(null, "[DOCKER] " + message.getContent());
            } else {
                biConsumer.accept(type.toString(), message.getContent());
            }
        }
    }

    /**
     * Supplies text and stream to {@link MachineLogsBiConsumer} from {@link ProgressStatus}.
     */
    private class MachineProgressMonitor implements ProgressMonitor {

        private final ProgressLineFormatterImpl  formatter;
        private final BiConsumer<String, String> biConsumer;

        public MachineProgressMonitor(BiConsumer<String, String> biConsumer) {
            this.formatter = new ProgressLineFormatterImpl();
            this.biConsumer = biConsumer;
        }

        @Override
        public void updateProgress(ProgressStatus status) {
            biConsumer.accept(status.getStream(), formatter.format(status));
        }
    }

    /**
     * Forms new instance of {@link MachineLogEvent} and publish it via {@link RemoteEventService}.
     */
    private class MachineLogsBiConsumer implements BiConsumer<String, String> {

        private final String          machineName;
        private final RuntimeIdentity runtime;

        public MachineLogsBiConsumer(String machineName,
                                     RuntimeIdentity runtime) {
            this.machineName = machineName;
            this.runtime = runtime;
        }

        @Override
        public void accept(String stream, String text) {
            final MachineLogEvent logEvent = DtoFactory.newDto(MachineLogEvent.class)
                                                       .withRuntimeId(DtoConverter.asDto(runtime))
                                                       .withStream(stream)
                                                       .withText(text)
                                                       .withTime(ZonedDateTime.now().format(ISO_OFFSET_DATE_TIME))
                                                       .withMachineName(machineName);
            remoteEventService.publish(MACHINE_LOG_METHOD_NAME,
                                       logEvent,
                                       (event, scope) -> event.getRuntimeId()
                                                              .getWorkspaceId()
                                                              .equals(scope.get("workspaceId")));
        }
    }

}
