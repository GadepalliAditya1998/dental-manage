package com.dental.manage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Base entity with audit fields shared by all tables.
 * <p>
 * Every entity inherits: id (auto-increment), isDeleted, createdOn, createdBy, modifiedOn, modifiedBy.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "modified_on", nullable = false)
    private LocalDateTime modifiedOn;

    @Column(name = "modified_by", nullable = false)
    private String modifiedBy;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        this.createdOn = now;
        this.modifiedOn = now;
        if (this.createdBy == null) {
            this.createdBy = "SYSTEM";
        }
        if (this.modifiedBy == null) {
            this.modifiedBy = this.createdBy;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifiedOn = LocalDateTime.now(ZoneOffset.UTC);
        if (this.modifiedBy == null) {
            this.modifiedBy = "SYSTEM";
        }
    }
}
