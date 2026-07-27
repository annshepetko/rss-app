package com.commafeed.integration;

import com.commafeed.TestConstants;
import com.commafeed.backend.dao.FeedEntryDAO;
import com.commafeed.backend.dao.FeedEntryNoteDAO;
import com.commafeed.backend.dao.UserDAO;
import com.commafeed.backend.model.FeedEntry;
import com.commafeed.backend.model.FeedEntryNote;
import com.commafeed.backend.model.User;
import com.commafeed.frontend.model.Entries;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FeedEntryNoteIT extends BaseIT {

    @Inject FeedEntryNoteDAO feedEntryNoteDAO;

    @Inject FeedEntryDAO feedEntryDAO;

    @Inject UserDAO userDAO;

    @Inject EntityManager entityManager;

    @BeforeEach
    void setup() {
        initialSetup(TestConstants.ADMIN_USERNAME, TestConstants.ADMIN_PASSWORD);

        RestAssured.authentication =
                RestAssured.preemptive()
                        .basic(TestConstants.ADMIN_USERNAME, TestConstants.ADMIN_PASSWORD);
    }

    @Test
    @Transactional
    void shouldFindNoteByUserAndEntry() {
        Long subscriptionId = subscribeAndWaitForEntries(getFeedUrl());

        Entries entries = getFeedEntries(subscriptionId);

        Long entryId = Long.valueOf(entries.getEntries().getFirst().getId());

        User user = userDAO.findByName(TestConstants.ADMIN_USERNAME);

        FeedEntry entry = feedEntryDAO.findById(entryId);

        FeedEntryNote note = new FeedEntryNote();
        note.setUser(user);
        note.setEntry(entry);
        note.setComment("Test note");
        note.setRating(5);

        entityManager.persist(note);
        entityManager.flush();

        Optional<FeedEntryNote> result = feedEntryNoteDAO.findByUserAndEntry(user, entry);

        Assertions.assertTrue(result.isPresent());

        Assertions.assertEquals("Test note", result.get().getComment());

        Assertions.assertEquals(5, result.get().getRating());
    }
}
