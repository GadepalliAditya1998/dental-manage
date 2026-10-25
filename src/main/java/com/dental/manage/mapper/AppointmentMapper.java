package com.dental.manage.mapper;

import com.dental.manage.dto.response.AppointmentResponse;
import com.dental.manage.dto.response.CallRecordResponse;
import com.dental.manage.dto.response.DoctorResponse;
import com.dental.manage.dto.response.PatientResponse;
import com.dental.manage.dto.response.ServiceResponse;
import com.dental.manage.entity.Appointment;
import com.dental.manage.entity.CallRecord;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.Patient;
import com.dental.manage.entity.Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {

    @Mapping(target = "patient", expression = "java(toPatientResponse(appointment.getPatient()))")
    @Mapping(target = "doctor", expression = "java(toDoctorResponse(appointment.getDoctor()))")
    @Mapping(target = "service", expression = "java(toServiceResponse(appointment.getService()))")
    @Mapping(target = "callRecord", expression = "java(toCallRecordResponse(appointment.getCallRecord()))")
    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponseList(List<Appointment> appointments);

    default PatientResponse toPatientResponse(Patient patient) {
        if (patient == null) return null;
        return PatientResponse.builder()
                .id(patient.getId())
                .clinicId(patient.getClinicId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .email(patient.getEmail())
                .phoneNumber(patient.getPhoneNumber())
                .dateOfBirth(patient.getDateOfBirth())
                .isNewLead(patient.getIsNewLead())
                .createdOn(patient.getCreatedOn())
                .modifiedOn(patient.getModifiedOn())
                .build();
    }

    default DoctorResponse toDoctorResponse(Doctor doctor) {
        if (doctor == null) return null;
        return DoctorResponse.builder()
                .id(doctor.getId())
                .clinicId(doctor.getClinicId())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .specialization(doctor.getSpecialization())
                .email(doctor.getEmail())
                .phoneNumber(doctor.getPhoneNumber())
                .isActive(doctor.getIsActive())
                .createdOn(doctor.getCreatedOn())
                .build();
    }

    default ServiceResponse toServiceResponse(Service service) {
        if (service == null) return null;
        return ServiceResponse.builder()
                .id(service.getId())
                .clinicId(service.getClinicId())
                .name(service.getName())
                .description(service.getDescription())
                .durationMinutes(service.getDurationMinutes())
                .price(service.getPrice())
                .isActive(service.getIsActive())
                .build();
    }

    default CallRecordResponse toCallRecordResponse(CallRecord callRecord) {
        if (callRecord == null) return null;
        return CallRecordResponse.builder()
                .id(callRecord.getId())
                .clinicId(callRecord.getClinicId())
                .retellCallId(callRecord.getRetellCallId())
                .callType(callRecord.getCallType() != null ? callRecord.getCallType().name() : null)
                .callStatus(callRecord.getCallStatus())
                .callerPhone(callRecord.getCallerPhone())
                .durationMs(callRecord.getDurationMs())
                .transcript(callRecord.getTranscript())
                .recordingUrl(callRecord.getRecordingUrl())
                .sentiment(callRecord.getSentiment())
                .sentimentScore(callRecord.getSentimentScore())
                .callSummary(callRecord.getCallSummary())
                .transferredToHuman(callRecord.getTransferredToHuman())
                .humanCallRecordingUrl(callRecord.getHumanCallRecordingUrl())
                .humanCallTranscript(callRecord.getHumanCallTranscript())
                .callStartedAt(callRecord.getCallStartedAt())
                .callEndedAt(callRecord.getCallEndedAt())
                .createdOn(callRecord.getCreatedOn())
                .build();
    }
}
