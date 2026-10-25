package com.dental.manage.controller;

import com.dental.manage.service.WhatsAppResponseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TwilioWhatsAppWebhookControllerTest {

    @Mock
    private WhatsAppResponseService whatsAppResponseService;

    @InjectMocks
    private TwilioWhatsAppWebhookController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/webhooks/twilio/whatsapp processes message and returns 200 with TwiML")
    void shouldHandleIncomingWhatsAppMessage() throws Exception {
        when(whatsAppResponseService.processInboundMessage("whatsapp:+15551234567", "1"))
                .thenReturn("Thank you! Your appointment is confirmed.");

        mockMvc.perform(post("/api/webhooks/twilio/whatsapp")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("From", "whatsapp:+15551234567")
                        .param("Body", "1")
                        .param("MessageSid", "SM123456789"))
                .andExpect(status().isOk())
                .andExpect(content().string("<Response></Response>"));

        verify(whatsAppResponseService).processInboundMessage(eq("whatsapp:+15551234567"), eq("1"));
    }

    @Test
    @DisplayName("POST /api/webhooks/twilio/whatsapp returns 400 when From or Body is missing")
    void shouldReturnBadRequestWhenParamsMissing() throws Exception {
        mockMvc.perform(post("/api/webhooks/twilio/whatsapp")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("From", "whatsapp:+15551234567"))
                .andExpect(status().isBadRequest());
    }
}
