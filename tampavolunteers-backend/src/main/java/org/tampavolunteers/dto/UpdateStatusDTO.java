package org.tampavolunteers.dto;

import lombok.Data;
import org.tampavolunteers.model.UserStatus;

@Data
public class UpdateStatusDTO {
    private UserStatus userStatus;
}
