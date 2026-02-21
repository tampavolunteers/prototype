package org.tampavolunteers.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.tampavolunteers.model.OrganizationMember.OrgMemberRole;

@Data
public class MemberRoleDTO {

    @NotNull(message = "role is required")
    private OrgMemberRole role;
}
