package com.dental.manage.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Request payload for patient lookup.
 * Supports searching via:
 * 1) Unique string patientId
 * OR
 * 2) Patient's mobile number and date of birth.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientLookupRequest {

    @JsonAlias({"patient_id", "id"})
    private String patientId;

    @JsonAlias({"mobileNumber", "mobile_number", "mobile", "phone", "phone_number"})
    private String phoneNumber;

    @JsonAlias({"name", "patientName"})
    private String fullName;

    @Past(message = "Date of birth must be in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonAlias({"dob", "date_of_birth"})
    private LocalDate dateOfBirth;

    @JsonAlias({"clinic_id"})
    private Long clinicId;
}
