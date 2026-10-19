package com.dental.manage.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO returned by the patient lookup API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientLookupResponse {

    private Long id;
    private Long clinicId;
    private String username;
    private String fullName;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private String role;
    private Boolean isActive;
    private LocalDateTime createdOn;
    private LocalDateTime modifiedOn;
}
