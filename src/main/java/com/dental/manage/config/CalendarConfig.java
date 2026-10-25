package com.dental.manage.config;

import com.dental.manage.enums.CalendarProviderType;
import com.dental.manage.service.CalendarService;
import com.dental.manage.service.impl.GoogleCalendarServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration registering the primary CalendarService bean.
 */
@Slf4j
@Configuration
public class CalendarConfig {

    @Bean
    @Primary
    public CalendarService calendarService(
            CalendarProperties properties,
            GoogleCalendarServiceImpl googleCalendarService) {

        if (properties.getProvider() == CalendarProviderType.GOOGLE) {
            log.info("Primary CalendarService configured: Google Calendar (calendarId={})",
                    properties.getGoogle().getCalendarId());
        } else {
            log.info("Calendar provider is set to: NONE");
        }
        return googleCalendarService;
    }
}
