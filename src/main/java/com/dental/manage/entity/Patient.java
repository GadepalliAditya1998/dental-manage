package com.dental.manage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Patient / Customer entity.
 */
@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone_number", nullable = false, unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "is_new_lead")
    @Builder.Default
    private Boolean isNewLead = true;

    @OneToMany(mappedBy = "patient")
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "patient")
    @Builder.Default
    private List<CallRecord> callRecords = new ArrayList<>();
}
