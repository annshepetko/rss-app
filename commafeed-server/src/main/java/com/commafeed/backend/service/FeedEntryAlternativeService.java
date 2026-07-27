package com.commafeed.backend.service;

import com.commafeed.CommaFeedConfiguration;
import com.commafeed.backend.dao.FeedEntryDAO;
import com.commafeed.backend.dao.FeedEntryStatusDAO;
import com.commafeed.backend.dao.FeedSubscriptionDAO;
import com.commafeed.backend.model.FeedEntry;
import com.commafeed.backend.model.FeedEntryContent;
import com.commafeed.backend.model.FeedEntryStatus;
import com.commafeed.backend.model.FeedSubscription;
import com.commafeed.backend.model.User;
import com.commafeed.frontend.OriginalContentDto;
import com.commafeed.frontend.model.Entry;
import com.commafeed.frontend.model.GenerateAlternativeResponse;
import com.commafeed.frontend.model.request.Target;
import jakarta.inject.Singleton;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
@Singleton
public class FeedEntryAlternativeService {

    private final FeedEntryDAO feedEntryDAO;
    private final FeedSubscriptionDAO feedSubscriptionDAO;
    private final FeedEntryStatusDAO feedEntryStatusDAO;
    private final LLMClient llmClient;
    private final CommaFeedConfiguration config;

    public GenerateAlternativeResponse generateAlternative(
            User user, GenerateAlternativeCommand command) {
        FeedEntry entry = getFeedEntry(command.entryId());

        String textToRewrite = extractText(entry, command.target());

        String generatedAlternative =
                llmClient.generateAlternative(command.prompt(), textToRewrite);

        System.out.println(generatedAlternative);

        Entry entryModel = buildEntry(user, entry);

        return buildResponse(entryModel, command, generatedAlternative);
    }

    private FeedEntry getFeedEntry(Long entryId) {
        FeedEntry entry = feedEntryDAO.findById(entryId);

        if (entry == null) {
            throw new NotFoundException("FeedEntry not found: " + entryId);
        }

        return entry;
    }

    private String extractText(FeedEntry entry, Target target) {
        FeedEntryContent content = entry.getContent();

        if (content == null) {
            throw new IllegalStateException(
                    "FeedEntryContent is missing for entry: " + entry.getId());
        }

        String text =
                switch (target) {
                    case TITLE -> content.getTitle();
                    case CONTENT -> content.getContent();
                };

        if (StringUtils.isBlank(text)) {
            throw new IllegalArgumentException(
                    "Selected target '" + target.name().toLowerCase() + "' is empty.");
        }

        return text;
    }

    private Entry buildEntry(User user, FeedEntry entry) {
        FeedSubscription subscription = feedSubscriptionDAO.findByFeed(user, entry.getFeed());

        FeedEntryStatus status = feedEntryStatusDAO.getStatus(user, subscription, entry);

        return Entry.build(status, config.imageProxyEnabled());
    }

    private GenerateAlternativeResponse buildResponse(
            Entry originalEntry, GenerateAlternativeCommand command, String generatedAlternative) {

        GenerateAlternativeResponse response = new GenerateAlternativeResponse();

        response.setOriginal(
                new OriginalContentDto(
                        originalEntry.getId(), originalEntry.getTitle(), originalEntry.getTitle()));
        response.setTarget(command.target());
        response.setPrompt(command.prompt());
        response.setGeneratedAlternative(generatedAlternative);

        return response;
    }
}
