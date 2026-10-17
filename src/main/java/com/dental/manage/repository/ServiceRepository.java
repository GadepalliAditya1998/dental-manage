package com.dental.manage.repository;

import com.dental.manage.entity.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    Optional<Service> findByIdAndIsDeletedFalse(Long id);

    List<Service> findByIsActiveTrueAndIsDeletedFalse();

    Page<Service> findByIsDeletedFalse(Pageable pageable);

    List<Service> findByNameContainingIgnoreCaseAndIsActiveTrueAndIsDeletedFalse(String name);
}
