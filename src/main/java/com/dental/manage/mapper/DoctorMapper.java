package com.dental.manage.mapper;

import com.dental.manage.dto.request.DoctorAvailabilityRequest;
import com.dental.manage.dto.request.DoctorRequest;
import com.dental.manage.dto.response.DoctorAvailabilityResponse;
import com.dental.manage.dto.response.DoctorResponse;
import com.dental.manage.entity.Doctor;
import com.dental.manage.entity.DoctorAvailability;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DoctorMapper {

    DoctorResponse toResponse(Doctor doctor);

    List<DoctorResponse> toResponseList(List<Doctor> doctors);

    Doctor toEntity(DoctorRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(DoctorRequest request, @MappingTarget Doctor doctor);

    DoctorAvailabilityResponse toAvailabilityResponse(DoctorAvailability availability);

    List<DoctorAvailabilityResponse> toAvailabilityResponseList(List<DoctorAvailability> availabilities);

    DoctorAvailability toAvailabilityEntity(DoctorAvailabilityRequest request);
}
