package org.eclipse.che.api.workspace.shared.dto.event;

import org.eclipse.che.api.workspace.shared.dto.RuntimeIdentityDto;
import org.eclipse.che.dto.shared.DTO;

/**
 * Defines event format for machine logs.
 *
 * @author Anton Korneta
 */
@DTO
public interface MachineLogEvent {

    String getText();

    void setText(String text);

    MachineLogEvent withText(String text);

    String getMachineName();

    void setMachineName(String machineName);

    MachineLogEvent withMachineName(String machineName);

    RuntimeIdentityDto getRuntimeId();

    void setRuntimeId(RuntimeIdentityDto runtimeId);

    MachineLogEvent withRuntimeId(RuntimeIdentityDto runtimeId);

}
