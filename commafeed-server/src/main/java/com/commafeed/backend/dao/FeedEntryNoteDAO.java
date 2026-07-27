package com.commafeed.backend.dao;

import com.commafeed.backend.model.FeedEntry;
import com.commafeed.backend.model.FeedEntryNote;
import com.commafeed.backend.model.QFeedEntryNote;
import com.commafeed.backend.model.User;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

@Singleton
public class FeedEntryNoteDAO extends GenericDAO<FeedEntryNote> {

    private static final QFeedEntryNote NOTE = QFeedEntryNote.feedEntryNote;

    public FeedEntryNoteDAO(EntityManager entityManager) {
        super(entityManager, FeedEntryNote.class);
    }

    public Optional<FeedEntryNote> findByUserAndEntry(User user, FeedEntry entry) {
        return Optional.ofNullable(
                query().selectFrom(NOTE)
                        .where(NOTE.user.eq(user), NOTE.entry.eq(entry))
                        .fetchOne());
    }

    public List<FeedEntryNote> findByUser(User user) {
        return query().selectFrom(NOTE).where(NOTE.user.eq(user)).fetch();
    }

    public FeedEntryNote findById(User user, Long id) {
        return query().selectFrom(NOTE).where(NOTE.user.eq(user), NOTE.id.eq(id)).fetchOne();
    }
}
