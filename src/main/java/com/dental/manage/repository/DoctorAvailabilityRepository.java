package com.dental.manage.repository;

import com.dental.manage.entity.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {

    List<DoctorAvailability> findByDoctorIdAndIsActiveTrueAndIsDeletedFalse(Long doctorId);

    List<DoctorAvailability> findByDoctorIdAndDayOfWeekAndIsActiveTrueAndIsDeletedFalse(
            Long doctorId, String dayOfWeek);

    List<DoctorAvailability> findByDoctorIdAndIsDeletedFalse(Long doctorId);

    void deleteByDoctorId(Long doctorId);
}
