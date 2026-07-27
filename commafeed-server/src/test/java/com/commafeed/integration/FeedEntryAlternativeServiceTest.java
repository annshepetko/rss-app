package com.commafeed.integration;

import com.commafeed.TestConstants;
import com.commafeed.backend.dao.UserDAO;
import com.commafeed.backend.model.User;
import com.commafeed.backend.service.FeedEntryAlternativeService;
import com.commafeed.backend.service.GenerateAlternativeCommand;
import com.commafeed.backend.service.internal.GroqClient;
import com.commafeed.frontend.OriginalContentDto;
import com.commafeed.frontend.model.Entries;
import com.commafeed.frontend.model.GenerateAlternativeResponse;
import com.commafeed.frontend.model.request.Target;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@QuarkusTest
class FeedEntryAlternativeServiceTest extends BaseIT {

    @Inject FeedEntryAlternativeService service;

    @Inject UserDAO userDAO;

    private GroqClient geminiClient;

    @BeforeEach
    void setup() {
        geminiClient = Mockito.mock(GroqClient.class);

        Mockito.when(geminiClient.generateAlternative(Mockito.anyString(), Mockito.anyString()))
                .thenReturn("Generated title");

        QuarkusMock.installMockForType(geminiClient, GroqClient.class);

        initialSetup(TestConstants.ADMIN_USERNAME, TestConstants.ADMIN_PASSWORD);

        RestAssured.authentication =
                RestAssured.preemptive()
                        .basic(TestConstants.ADMIN_USERNAME, TestConstants.ADMIN_PASSWORD);
    }

    @AfterEach
    void cleanup() {
        RestAssured.reset();
    }

    @Test
    void shouldGenerateAlternativeForTitle() {
        Long subscriptionId = subscribeAndWaitForEntries(getFeedUrl());

        Entries entries = getFeedEntries(subscriptionId);

        Long entryId = Long.valueOf(entries.getEntries().getFirst().getId());

        User user = userDAO.findByName(TestConstants.ADMIN_USERNAME);

        GenerateAlternativeCommand command =
                new GenerateAlternativeCommand(entryId, Target.TITLE, "Rewrite professionally");

        GenerateAlternativeResponse response = service.generateAlternative(user, command);

        Assertions.assertNotNull(response);

        Assertions.assertEquals(Target.TITLE, response.getTarget());

        Assertions.assertEquals("Rewrite professionally", response.getPrompt());

        Assertions.assertEquals("Generated title", response.getGeneratedAlternative());

        OriginalContentDto original = response.getOriginal();

        Assertions.assertNotNull(original);

        Assertions.assertEquals(String.valueOf(entryId), original.getEntryId());

        Mockito.verify(geminiClient)
                .generateAlternative("Rewrite professionally", original.getTitle());
    }
}
