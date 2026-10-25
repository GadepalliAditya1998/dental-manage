package com.dental.manage.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PatientIdGeneratorTest {

    @Test
    @DisplayName("Generated patient ID should start with prefix PAT-<clinicId>-")
    void generate_withClinicId_shouldHaveCorrectPrefix() {
        String patientId = PatientIdGenerator.generate(5L);

        assertThat(patientId).isNotNull();
        assertThat(patientId).startsWith("PAT-5-");
        assertThat(patientId.length()).isGreaterThan(10);
    }

    @Test
    @DisplayName("Generated patient ID with null clinicId defaults to clinic 1")
    void generate_withNullClinicId_shouldDefaultToClinic1() {
        String patientId = PatientIdGenerator.generate(null);

        assertThat(patientId).isNotNull();
        assertThat(patientId).startsWith("PAT-1-");
    }

    @Test
    @DisplayName("Generated patient IDs should be unique across multiple invocations")
    void generate_consecutiveCalls_shouldBeUnique() {
        Set<String> generatedIds = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String id = PatientIdGenerator.generate(1L);
            assertThat(generatedIds.add(id)).as("ID %s should be unique", id).isTrue();
        }
    }

    @Test
    @DisplayName("extractClinicId should extract the clinic identifier correctly")
    void extractClinicId_validPatientId_shouldReturnClinicId() {
        String patientId = "PAT-42-000192B6E9F8A3B4";
        Long clinicId = PatientIdGenerator.extractClinicId(patientId);

        assertThat(clinicId).isEqualTo(42L);
    }

    @Test
    @DisplayName("extractClinicId with invalid format returns null")
    void extractClinicId_invalidFormat_shouldReturnNull() {
        assertThat(PatientIdGenerator.extractClinicId(null)).isNull();
        assertThat(PatientIdGenerator.extractClinicId("INVALID-ID")).isNull();
        assertThat(PatientIdGenerator.extractClinicId("PAT-NOTANUMBER-XYZ")).isNull();
    }
}
