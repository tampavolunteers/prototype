package org.tampavolunteers.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.tampavolunteers.model.User;

@Data
public class ChangeRoleDTO {

    @NotNull(message = "role is required")
    private User.UserRole role;
}
