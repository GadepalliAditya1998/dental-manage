package com.dental.manage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/**
 * Clinic configuration entity supporting multi-tenant architecture.
 */
@Entity
@Table(name = "clinic_details")
@Getter
@Setter
@AllArgsConstructor
@Builder
public class ClinicDetails extends BaseEntity {

    public ClinicDetails() {
    }

    @Column(name = "clinic_id")
    private Long clinicId;

    @Column(name = "clinic_name", nullable = false)
    private String clinicName;

    @Column(name = "clinic_phone", length = 20)
    private String clinicPhone;

    @Column(name = "receptionist_phone", length = 20)
    private String receptionistPhone;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(name = "working_days", length = 100)
    private String workingDays;

    @Column(name = "retell_agent_id")
    private String retellAgentId;

    @Column(name = "timezone", length = 50)
    private String timezone;
}
