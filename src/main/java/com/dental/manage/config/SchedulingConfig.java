package com.dental.manage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled task execution for automated background jobs
 * such as daily WhatsApp appointment reminders.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
