package com.dental.manage.service.provider;

import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.Patient;
import com.dental.manage.entity.Service;
import com.dental.manage.service.provider.impl.TwilioWhatsAppProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TwilioWhatsAppProviderTest {

    private TwilioWhatsAppProvider provider;

    @BeforeEach
    void setUp() {
        provider = new TwilioWhatsAppProvider();
        ReflectionTestUtils.setField(provider, "clinicName", "Smile Dental Clinic");
        ReflectionTestUtils.setField(provider, "defaultCountryCode", "+1");
        ReflectionTestUtils.setField(provider, "accountSid", "");
        ReflectionTestUtils.setField(provider, "authToken", "");
        ReflectionTestUtils.setField(provider, "twilioWhatsAppNumber", "");
    }

    @Test
    @DisplayName("formatWhatsAppNumber normalizes 10-digit US phone numbers with +1")
    void shouldFormatTenDigitUsNumber() {
        String formatted = provider.formatWhatsAppNumber("5551234567");
        assertThat(formatted).isEqualTo("whatsapp:+15551234567");
    }

    @Test
    @DisplayName("formatWhatsAppNumber handles numbers with hyphens and spaces")
    void shouldFormatFormattedUsNumber() {
        String formatted = provider.formatWhatsAppNumber("(555) 123-4567");
        assertThat(formatted).isEqualTo("whatsapp:+15551234567");
    }

    @Test
    @DisplayName("formatWhatsAppNumber handles 11-digit numbers starting with 1")
    void shouldFormatElevenDigitUsNumber() {
        String formatted = provider.formatWhatsAppNumber("15551234567");
        assertThat(formatted).isEqualTo("whatsapp:+15551234567");
    }

    @Test
    @DisplayName("formatWhatsAppNumber preserves international numbers with plus")
    void shouldFormatInternationalNumber() {
        String formatted = provider.formatWhatsAppNumber("+447911123456");
        assertThat(formatted).isEqualTo("whatsapp:+447911123456");
    }

    @Test
    @DisplayName("formatWhatsAppNumber strips existing whatsapp: prefix safely")
    void shouldStripExistingPrefix() {
        String formatted = provider.formatWhatsAppNumber("whatsapp:+15551234567");
        assertThat(formatted).isEqualTo("whatsapp:+15551234567");
    }

    @Test
    @DisplayName("formatWhatsAppNumber returns null for null or empty input")
    void shouldReturnNullForEmptyPhone() {
        assertThat(provider.formatWhatsAppNumber(null)).isNull();
        assertThat(provider.formatWhatsAppNumber("   ")).isNull();
    }

    @Test
    @DisplayName("sendAppointmentReminder returns false if appointment is null")
    void shouldReturnFalseForNullAppointment() {
        boolean result = provider.sendAppointmentReminder(null);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("sendAppointmentReminder returns false if patient has opted out")
    void shouldReturnFalseForOptedOutPatient() {
        Patient patient = Patient.builder()
                .phoneNumber("5551234567")
                .whatsappOptOut(true)
                .build();

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .build();

        boolean result = provider.sendAppointmentReminder(appointment);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("sendAppointmentReminder returns false if patient phone is missing")
    void shouldReturnFalseWhenPhoneMissing() {
        Patient patient = Patient.builder()
                .phoneNumber(null)
                .build();

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .build();

        boolean result = provider.sendAppointmentReminder(appointment);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("sendMessage returns false safely when Twilio credentials are not configured")
    void shouldHandleUnconfiguredTwilioGracefully() {
        boolean sent = provider.sendMessage("5551234567", "Test reminder message");
        assertThat(sent).isFalse();
    }
}
