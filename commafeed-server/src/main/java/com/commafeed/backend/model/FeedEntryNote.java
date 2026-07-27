package com.commafeed.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "FEEDENTRYNOTES")
@SuppressWarnings("serial")
@Getter
@Setter
public class FeedEntryNote extends AbstractModel {

    @JoinColumn(name = "user_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @JoinColumn(name = "entry_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private FeedEntry entry;

    @Column(name = "comment", length = 1000, nullable = false)
    private String comment;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    public FeedEntryNote() {}

    public FeedEntryNote(User user, FeedEntry entry, String comment, Integer rating) {
        this.user = user;
        this.entry = entry;
        this.comment = comment;
        this.rating = rating;
    }
}
