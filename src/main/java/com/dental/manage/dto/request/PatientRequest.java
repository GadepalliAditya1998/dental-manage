package com.dental.manage.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Request DTO for creating and updating patient records.
 * Supports flexible attribute aliases (e.g. name, patientName, phoneNumber, phone, dateofbirth).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRequest {

    @JsonAlias({"clinic_id"})
    private Long clinicId;

    @JsonAlias({"patient_id", "patientId", "id"})
    private String patientId;

    @JsonAlias({"patientName", "patient_name", "patient name", "fullName", "full_name", "full name"})
    private String name;

    @JsonAlias({"first_name"})
    private String firstName;

    @JsonAlias({"last_name"})
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone number is required")
    @JsonAlias({"phone", "phone_number", "phone number"})
    private String phoneNumber;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonAlias({"dateofbirth", "date_of_birth", "dob", "date of birth"})
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    @JsonAlias({"whatsapp_opt_out", "whatsappOptOut", "optOutWhatsApp"})
    private Boolean whatsappOptOut;

    @JsonAlias({"is_new_lead", "isNewLead", "newLead"})
    private Boolean isNewLead;

    /**
     * Custom setter for name to populate firstName and lastName if not explicitly set.
     */
    public void setName(String name) {
        this.name = name;
        if ((this.firstName == null || this.firstName.isBlank()) && name != null && !name.isBlank()) {
            String[] parts = name.trim().split("\\s+", 2);
            this.firstName = parts[0];
            if (this.lastName == null || this.lastName.isBlank()) {
                this.lastName = parts.length > 1 ? parts[1].trim() : "";
            }
        }
    }

    /**
     * Resolves the patient's first name.
     */
    public String getEffectiveFirstName() {
        if (firstName != null && !firstName.isBlank()) {
            return firstName.trim();
        }
        if (name != null && !name.isBlank()) {
            String[] parts = name.trim().split("\\s+", 2);
            return parts[0].trim();
        }
        return null;
    }

    /**
     * Resolves the patient's last name.
     */
    public String getEffectiveLastName() {
        if (lastName != null && !lastName.isBlank()) {
            return lastName.trim();
        }
        if (name != null && !name.isBlank()) {
            String[] parts = name.trim().split("\\s+", 2);
            return parts.length > 1 ? parts[1].trim() : "";
        }
        return "";
    }

    /**
     * Resolves the patient's full name.
     */
    public String getEffectiveFullName() {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        String f = firstName != null ? firstName.trim() : "";
        String l = lastName != null ? lastName.trim() : "";
        return (f + " " + l).trim();
    }
}
