package com.ecommerce.notification_service;

import com.ecommerce.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * application-test.yml excludes MongoAutoConfiguration so no real MongoDB is needed.
 * @MockitoBean on the repository prevents the UnsatisfiedDependencyException that
 * would occur because MongoRepositoriesAutoConfiguration is also excluded.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

    @MockitoBean
    private NotificationRepository notificationRepository;

    @Test
    void contextLoads() {}
}
