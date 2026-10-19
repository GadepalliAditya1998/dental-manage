package com.dental.manage.mapper;

import com.dental.manage.dto.request.ClinicSettingsRequest;
import com.dental.manage.dto.response.ClinicSettingsResponse;
import com.dental.manage.dto.response.ClinicWorkingHoursResponse;
import com.dental.manage.entity.ClinicDetails;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for converting between ClinicDetails entities and DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClinicDetailsMapper {

    @Mapping(target = "period", ignore = true)
    @Mapping(target = "workingHours", ignore = true)
    ClinicWorkingHoursResponse toWorkingHoursResponse(ClinicDetails clinicDetails);

    ClinicSettingsResponse toSettingsResponse(ClinicDetails clinicDetails);

    ClinicDetails toEntity(ClinicSettingsRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(ClinicSettingsRequest request, @MappingTarget ClinicDetails clinicDetails);
}
