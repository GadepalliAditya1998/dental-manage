package com.dental.manage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Available time slots for a doctor on a specific date.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotResponse {

    private Long doctorId;
    private String doctorName;
    private String date;
    private List<SlotInfo> availableSlots;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SlotInfo {
        private LocalTime startTime;
        private LocalTime endTime;
        private String displayTime;
    }
}
