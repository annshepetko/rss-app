package com.commafeed.backend.service;

import com.commafeed.backend.dao.FeedEntryDAO;
import com.commafeed.backend.dao.FeedEntryNoteDAO;
import com.commafeed.backend.model.FeedEntry;
import com.commafeed.backend.model.FeedEntryNote;
import com.commafeed.backend.model.User;
import com.commafeed.frontend.model.request.AddNoteRequest;
import jakarta.inject.Singleton;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Singleton
public class FeedEntryNoteService {

    private final FeedEntryDAO feedEntryDAO;
    private final FeedEntryNoteDAO feedEntryNoteDAO;

    public void addNote(User user, AddNoteRequest req) {
        FeedEntry entry = feedEntryDAO.findById(req.getEntryId());
        if (entry == null) {
            throw new IllegalArgumentException("Entry not found: " + req.getEntryId());
        }

        FeedEntryNote note = new FeedEntryNote(user, entry, req.getComment(), req.getRating());
        feedEntryNoteDAO.persist(note);
    }

    public List<FeedEntryNote> getNotes(User user) {
        return feedEntryNoteDAO.findByUser(user);
    }

    public FeedEntryNote getNote(User user, Long id) {
        return feedEntryNoteDAO.findById(user, id);
    }
}
