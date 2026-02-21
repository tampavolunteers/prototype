package org.tampavolunteers.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.tampavolunteers.model.OrganizationMember.OrgMemberRole;

@Data
public class InviteMemberDTO {

    @NotNull(message = "userId is required")
    private Long userId;

    private OrgMemberRole role = OrgMemberRole.MEMBER;
}
