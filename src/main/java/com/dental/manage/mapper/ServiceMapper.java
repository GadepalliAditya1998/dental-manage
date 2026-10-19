package com.dental.manage.mapper;

import com.dental.manage.dto.request.ServiceRequest;
import com.dental.manage.dto.response.ServiceResponse;
import com.dental.manage.entity.Service;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceMapper {

    ServiceResponse toResponse(Service service);

    List<ServiceResponse> toResponseList(List<Service> services);

    Service toEntity(ServiceRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(ServiceRequest request, @MappingTarget Service service);
}
