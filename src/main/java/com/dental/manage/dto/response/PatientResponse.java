package com.dental.manage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO representing a patient record, including user account credentials.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponse {

    private Long id;
    private String patientId;
    private Long clinicId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String username;
    private String password;
    private String email;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private Boolean isNewLead;
    private Boolean whatsappOptOut;
    private LocalDateTime createdOn;
    private LocalDateTime modifiedOn;
}
