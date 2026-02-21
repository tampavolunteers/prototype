package org.tampavolunteers.dto;

import lombok.Data;

@Data
public class UpdateProfileDTO {
    private String firstName;
    private String lastName;
    private String bio;
    private String avatarUrl;
    private String phone;
}
