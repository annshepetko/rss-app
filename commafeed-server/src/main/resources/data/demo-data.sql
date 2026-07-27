-- Feed
INSERT INTO FEEDS (
    id,
    url,
    url_after_redirect,
    normalizedUrl,
    normalizedUrlHash,
    link,
    icon_url,
    lastUpdated,
    lastPublishedDate,
    lastEntryDate,
    message,
    errorCount,
    disabledUntil,
    lastModifiedHeader,
    etagHeader,
    averageEntryInterval,
    lastContentHash
) VALUES (
             1,
             'https://example.com/feed.xml',
             'https://example.com/feed.xml',
             'https://example.com/feed.xml',
             'feedhash12345678901234567890123456789012',
             'https://example.com',
             'https://example.com/favicon.ico',
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP,
             NULL,
             0,
             NULL,
             'Mon, 27 Jul 2026 10:00:00 GMT',
             'etag-123',
             86400,
             'contenthash1234567890123456789012345678'
         );

-- Category
INSERT INTO FEEDCATEGORIES (
    id,
    name,
    user_id,
    parent_id,
    collapsed,
    position
) VALUES (
             1,
             'Technology',
             2,
             NULL,
             FALSE,
             0
         );

-- Subscription
INSERT INTO FEEDSUBSCRIPTIONS (
    id,
    user_id,
    feed_id,
    title,
    category_id,
    position,
    filtering_expression,
    filtering_expression_legacy,
    push_notifications_enabled,
    auto_mark_as_read_after_days
) VALUES (
             1,
             2,
             1,
             'Example Feed',
             1,
             0,
             NULL,
             NULL,
             TRUE,
             30
         );

-- Entry content
INSERT INTO FEEDENTRYCONTENTS (
    id,
    title,
    titleHash,
    content,
    contentHash,
    author,
    enclosureUrl,
    enclosureType,
    mediaDescription,
    mediaThumbnailUrl,
    mediaThumbnailWidth,
    mediaThumbnailHeight,
    categories,
    direction
) VALUES (
             1,
             'First article',
             'titlehash123456789012345678901234567890',
             '<p>This is demo content.</p>',
             'contenthash9876543210987654321098765432',
             'John Doe',
             NULL,
             NULL,
             NULL,
             NULL,
             NULL,
             NULL,
             'Technology,Java',
             'LTR'
         );

-- Entry
INSERT INTO FEEDENTRIES (
    id,
    guid,
    guidHash,
    feed_id,
    content_id,
    url,
    inserted,
    updated
) VALUES (
             1,
             'demo-guid-1',
             'guidhash1234567890123456789012345678901',
             1,
             1,
             'https://example.com/articles/1',
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP
         );

-- Status
INSERT INTO FEEDENTRYSTATUSES (
    id,
    subscription_id,
    entry_id,
    read_status,
    starred,
    user_id,
    entryInserted,
    entryUpdated
) VALUES (
             1,
             1,
             1,
             FALSE,
             FALSE,
             2,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP
         );

-- Tag
INSERT INTO FEEDENTRYTAGS (
    id,
    user_id,
    entry_id,
    name
) VALUES (
             1,
             2,
             1,
             'demo'
         );