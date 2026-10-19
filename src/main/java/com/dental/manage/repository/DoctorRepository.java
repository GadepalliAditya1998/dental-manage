package com.dental.manage.repository;

import com.dental.manage.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByIdAndIsDeletedFalse(Long id);

    List<Doctor> findByIsActiveTrueAndIsDeletedFalse();

    Page<Doctor> findByIsDeletedFalse(Pageable pageable);

    @Query("SELECT DISTINCT d FROM Doctor d JOIN d.doctorServices ds " +
           "WHERE ds.service.id = :serviceId AND d.isActive = true AND d.isDeleted = false")
    List<Doctor> findByServiceId(@Param("serviceId") Long serviceId);

    // Multi-tenant queries
    List<Doctor> findByClinicIdAndIsActiveTrueAndIsDeletedFalse(Long clinicId);

    List<Doctor> findByClinicIdAndIsDeletedFalse(Long clinicId);

    Optional<Doctor> findByIdAndClinicIdAndIsDeletedFalse(Long id, Long clinicId);

    Page<Doctor> findByClinicIdAndIsDeletedFalse(Long clinicId, Pageable pageable);

    @Query("SELECT DISTINCT d FROM Doctor d JOIN d.doctorServices ds " +
           "WHERE d.clinicId = :clinicId AND ds.service.id = :serviceId AND d.isActive = true AND d.isDeleted = false")
    List<Doctor> findByClinicIdAndServiceId(@Param("clinicId") Long clinicId, @Param("serviceId") Long serviceId);
}
