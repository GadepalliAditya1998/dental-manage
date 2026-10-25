package com.dental.manage.service;

/**
 * Service for handling inbound WhatsApp responses from patients
 * (e.g., confirmations, reschedule requests, cancellations, and opt-outs).
 */
public interface WhatsAppResponseService {

    /**
     * Processes an inbound WhatsApp message from a patient phone number.
     * Parses their choice (1: Confirm, 2: Reschedule, 3: Cancel, 4: Opt-out)
     * and performs the appropriate entity updates and sends an auto-reply.
     *
     * @param fromPhone Patient's WhatsApp phone number
     * @param messageBody Text content of the incoming message
     * @return Generated auto-reply text sent to the patient
     */
    String processInboundMessage(String fromPhone, String messageBody);
}
