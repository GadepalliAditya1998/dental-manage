package com.dental.manage.repository;

import com.dental.manage.entity.DoctorService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorServiceRepository extends JpaRepository<DoctorService, Long> {

    List<DoctorService> findByDoctorIdAndIsDeletedFalse(Long doctorId);

    List<DoctorService> findByServiceIdAndIsDeletedFalse(Long serviceId);

    void deleteByDoctorId(Long doctorId);

    // Multi-tenant queries
    List<DoctorService> findByClinicIdAndDoctorIdAndIsDeletedFalse(Long clinicId, Long doctorId);

    List<DoctorService> findByClinicIdAndServiceIdAndIsDeletedFalse(Long clinicId, Long serviceId);

    void deleteByClinicIdAndDoctorId(Long clinicId, Long doctorId);
}
