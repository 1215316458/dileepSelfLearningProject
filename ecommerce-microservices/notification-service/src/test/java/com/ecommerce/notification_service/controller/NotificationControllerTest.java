package com.ecommerce.notification_service.controller;

import com.ecommerce.notification_service.domain.NotificationType;
import com.ecommerce.notification_service.dto.CreateNotificationRequest;
import com.ecommerce.notification_service.dto.NotificationResponse;
import com.ecommerce.notification_service.exception.NotificationNotFoundException;
import com.ecommerce.notification_service.repository.NotificationRepository;
import com.ecommerce.notification_service.service.NotificationService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller integration tests.
 * @SpringBootTest(MOCK) + MockMvcBuilders — same pattern as user-service Day 18.
 * @MockitoBean replaces NotificationService in the context so no MongoDB is needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private NotificationService notificationService;

    // Also mock the repository so the context can wire NotificationService
    // even though MongoRepositoriesAutoConfiguration is excluded in test profile
    @MockitoBean
    private NotificationRepository notificationRepository;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    private final ObjectMapper mapper = new ObjectMapper();

    private NotificationResponse sample() {
        return new NotificationResponse(
                "abc123", 1L, NotificationType.ORDER_PLACED,
                "Order Placed", "Your order #100 placed", false,
                Instant.now(), 100L
        );
    }

    @Test
    void createNotification_returns201() throws Exception {
        when(notificationService.create(any())).thenReturn(sample());

        CreateNotificationRequest req = new CreateNotificationRequest(
                1L, NotificationType.ORDER_PLACED, "Order Placed", "Your order #100 placed", 100L
        );

        mockMvc().perform(post("/api/notifications")
                        .contentType(APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.type").value("ORDER_PLACED"));
    }

    @Test
    void createNotification_returns400OnMissingFields() throws Exception {
        // missing required fields
        mockMvc().perform(post("/api/notifications")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByUser_returnsPaginatedNotifications() throws Exception {
        when(notificationService.getByUser(eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(sample())));

        mockMvc().perform(get("/api/notifications/user/1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1));
    }

    @Test
    void getUnread_returnsUnreadList() throws Exception {
        when(notificationService.getUnread(1L)).thenReturn(List.of(sample()));

        mockMvc().perform(get("/api/notifications/user/1/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void countUnread_returnsCount() throws Exception {
        when(notificationService.countUnread(1L)).thenReturn(5L);

        mockMvc().perform(get("/api/notifications/user/1/unread/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    void markRead_returns200() throws Exception {
        NotificationResponse readResponse = new NotificationResponse(
                "abc123", 1L, NotificationType.ORDER_PLACED,
                "Order Placed", "msg", true, Instant.now(), 100L
        );
        when(notificationService.markRead("abc123")).thenReturn(readResponse);

        mockMvc().perform(patch("/api/notifications/abc123/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markRead_returns404WhenNotFound() throws Exception {
        when(notificationService.markRead("missing"))
                .thenThrow(new NotificationNotFoundException("missing"));

        mockMvc().perform(patch("/api/notifications/missing/read"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(notificationService).delete("abc123");

        mockMvc().perform(delete("/api/notifications/abc123"))
                .andExpect(status().isNoContent());
    }
}
