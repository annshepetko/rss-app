package com.commafeed.backend.service;

import com.commafeed.CommaFeedConfiguration;
import com.commafeed.backend.dao.FeedEntryDAO;
import com.commafeed.backend.dao.FeedEntryStatusDAO;
import com.commafeed.backend.dao.FeedSubscriptionDAO;
import com.commafeed.backend.model.Feed;
import com.commafeed.backend.model.FeedEntry;
import com.commafeed.backend.model.FeedEntryContent;
import com.commafeed.backend.model.FeedEntryStatus;
import com.commafeed.backend.model.FeedSubscription;
import com.commafeed.backend.model.User;
import com.commafeed.frontend.model.GenerateAlternativeResponse;
import com.commafeed.frontend.model.request.Target;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedEntryAlternativeServiceTest {

    @Mock FeedEntryDAO feedEntryDAO;

    @Mock FeedSubscriptionDAO feedSubscriptionDAO;

    @Mock FeedEntryStatusDAO feedEntryStatusDAO;

    @Mock LLMClient llmClient;

    @Mock CommaFeedConfiguration config;

    @InjectMocks FeedEntryAlternativeService service;

    private User user;
    private FeedEntry entry;
    private FeedEntryContent content;
    private Feed feed;
    private FeedSubscription subscription;
    private FeedEntryStatus status;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);

        feed = new Feed();
        feed.setId(10L);

        content = new FeedEntryContent();
        content.setTitle("Original title");
        content.setContent("Original content");

        entry = new FeedEntry();
        entry.setId(100L);
        entry.setFeed(feed);
        entry.setContent(content);

        subscription = new FeedSubscription();
        subscription.setFeed(feed);

        status = new FeedEntryStatus();
        status.setEntry(entry);
        status.setSubscription(subscription);

        Mockito.when(config.imageProxyEnabled()).thenReturn(false);
    }

    @Test
    void shouldGenerateAlternativeForTitle() {
        Mockito.when(feedEntryDAO.findById(100L)).thenReturn(entry);

        Mockito.when(feedSubscriptionDAO.findByFeed(user, feed)).thenReturn(subscription);

        Mockito.when(feedEntryStatusDAO.getStatus(user, subscription, entry)).thenReturn(status);

        Mockito.when(llmClient.generateAlternative("Rewrite professionally", "Original title"))
                .thenReturn("Generated title");

        GenerateAlternativeCommand command =
                new GenerateAlternativeCommand(100L, Target.TITLE, "Rewrite professionally");

        GenerateAlternativeResponse response = service.generateAlternative(user, command);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(Target.TITLE, response.getTarget());
        Assertions.assertEquals("Rewrite professionally", response.getPrompt());
        Assertions.assertEquals("Generated title", response.getGeneratedAlternative());

        Assertions.assertNotNull(response);
        Assertions.assertEquals("100", response.getOriginal().getEntryId());

        Mockito.verify(feedEntryDAO).findById(100L);

        Mockito.verify(feedSubscriptionDAO).findByFeed(user, feed);

        Mockito.verify(feedEntryStatusDAO).getStatus(user, subscription, entry);

        Mockito.verify(llmClient).generateAlternative("Rewrite professionally", "Original title");
    }
}
