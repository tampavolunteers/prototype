package org.tampavolunteers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrganizationDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private String website;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Contact email must be valid")
    private String contactEmail;

    private String contactPhone;
    private String street;
    private String city;
    private String state;
    private String zip;
}
