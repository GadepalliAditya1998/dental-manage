package com.dental.manage.controller;

import com.dental.manage.service.WhatsAppResponseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound webhook endpoint for Twilio WhatsApp messaging events.
 * Receives patient responses when they reply to appointment confirmation reminders.
 */
@RestController
@RequestMapping("/api/webhooks/twilio")
@RequiredArgsConstructor
@Slf4j
public class TwilioWhatsAppWebhookController {

    private final WhatsAppResponseService whatsAppResponseService;

    /**
     * Webhook endpoint called by Twilio when a WhatsApp message is received.
     * Parses the incoming parameters and invokes WhatsAppResponseService.
     *
     * @param from Twilio "From" parameter (e.g. "whatsapp:+15551234567")
     * @param body Text message sent by the user (e.g. "1", "2", "3", "4")
     * @param messageSid Twilio MessageSid
     * @return TwiML response
     */
    @PostMapping(value = "/whatsapp", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.ALL_VALUE},
                 produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<String> handleIncomingWhatsAppMessage(
            @RequestParam(value = "From", required = false) String from,
            @RequestParam(value = "Body", required = false) String body,
            @RequestParam(value = "MessageSid", required = false) String messageSid) {

        log.info("Twilio WhatsApp Webhook received. From: {}, Body: {}, MessageSid: {}", from, body, messageSid);

        if (from == null || body == null) {
            log.warn("Missing required From or Body parameter in Twilio webhook");
            return ResponseEntity.badRequest().body("<Response><Message>Missing From or Body parameter</Message></Response>");
        }

        whatsAppResponseService.processInboundMessage(from, body);

        // Return empty TwiML response as reply has been dispatched via provider
        return ResponseEntity.ok("<Response></Response>");
    }
}
