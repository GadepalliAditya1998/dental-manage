package com.dental.manage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorResponse {

    private Long id;
    private Long clinicId;
    private String firstName;
    private String lastName;
    private String specialization;
    private String email;
    private String phoneNumber;
    private Boolean isActive;
    private LocalDateTime createdOn;
}
