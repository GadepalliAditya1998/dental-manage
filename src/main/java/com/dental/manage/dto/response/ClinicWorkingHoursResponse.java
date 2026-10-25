package com.dental.manage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Response containing clinic details along with day-wise working hours.
 * Formatted for frontend clients and AI conversational agents.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicWorkingHoursResponse {

    private Long id;
    private String clinicName;
    private String clinicPhone;
    private String receptionistPhone;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String workingDays;
    private String retellAgentId;
    private String timezone;
    private String apiKey;

    /**
     * Query period representation: "DAY" or "WEEK".
     */
    private String period;

    /**
     * Day-wise working hours records for a day or for a week.
     */
    private List<DayWorkingHours> workingHours;

    /**
     * Working hours details for an individual day.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DayWorkingHours {
        private String dayOfWeek;
        private LocalDateTime dateTime;
        private LocalTime openingTime;
        private LocalTime closingTime;
        private Boolean isOpen;
        private String status;
        private String formattedHours;
    }
}
