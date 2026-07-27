
# Implemented functionality

## Feed Entry Notes

Added functionality for creating and retrieving personal notes attached to feed entries.

Implemented:

* Creating a note for a feed entry
* Retrieving all notes belonging to the authenticated user
* Retrieving a single note by ID
* User-level authorization for notes

Notes are linked to:

* User
* Feed entry
* Comment text
* Rating

Only authenticated users with the `USER` role can access note endpoints.

---

# API Usage Flow

## 1. Register a user

Create a new user account.

### Request

```http
POST /rest/user/register
Content-Type: application/json

{
  "name": "test-user",
  "password": "password123",
  "email": "test@example.com"
}
```

### Response

```http
200 OK
```

---
## 2. Authenticate user

The API endpoints require authentication.

When testing the API using Postman:

1. Open the **Authorization** tab.
2. Select **Basic Auth** as the authentication type.
3. Provide the credentials of the registered user:

    * Username: the value used during registration (`name`)
    * Password: the password used during registration

Postman will automatically add the `Authorization` header to requests.

Example:

```http
Authorization: Basic <encoded_credentials>
```

After configuring Basic Auth, authenticated endpoints can be accessed:

* `POST /rest/note`
* `GET /rest/note`
* `GET /rest/note/{id}`
* `POST /rest/entry/{id}/generate-alternative`

All these endpoints require a user with the `USER` role.

All these endpoints require a user with the USER role.
---

# Notes API

## 3. Create a note for a feed entry

Creates a new note associated with a feed entry.

### Request

```http
POST /rest/note
Content-Type: application/json

{
  "entryId": 1,
  "comment": "Interesting article about Java",
  "rating": 5
}
```

### Response

```http
200 OK
```

---

## 4. Get all user notes

Returns all notes created by the current authenticated user.

### Request

```http
GET /rest/note
```

### Response

```json
[
  {
    "id": 1,
    "entryId": 1,
    "comment": "Interesting article about Java",
    "rating": 5
  }
]
```

---

## 5. Get note by ID

Returns a single note belonging to the authenticated user.

### Request

```http
GET /rest/note/1
```

### Response

```json
{
  "id": 1,
  "entryId": 1,
  "comment": "Interesting article about Java",
  "rating": 5
}
```

If the note does not exist or does not belong to the current user:

```http
404 Not Found
```

---

# AI Alternative Generation

## Generate alternative title or content using LLM

Added an endpoint that generates an alternative version of a feed entry title or content using an LLM.

The endpoint:

* Loads the feed entry
* Extracts the selected target (`TITLE` or `CONTENT`)
* Sends the original text and user prompt to the LLM client
* Returns the generated alternative together with the original content

---

## 6. Generate alternative title

Example: rewrite a feed entry title.

### Request

```http
POST /rest/entry/1/generate-alternative
Content-Type: application/json

{
  "target": "TITLE",
  "prompt": "Make the title shorter and more engaging"
}
```

### Response

```json
{
  "original": {
    "id": "1",
    "title": "Original article title",
    "content": "Original article content"
  },
  "target": "TITLE",
  "prompt": "Make the title shorter and more engaging",
  "generatedAlternative": "A shorter generated title"
}
```

---

## 7. Generate alternative content

Example: rewrite the article content.

### Request

```http
POST /rest/entry/1/generate-alternative
Content-Type: application/json

{
  "target": "CONTENT",
  "prompt": "Summarize this article in a clear way"
}
```

### Response

```json
{
  "original": {
    "id": "1",
    "title": "Original article title",
    "content": "Original article content"
  },
  "target": "CONTENT",
  "prompt": "Summarize this article in a clear way",
  "generatedAlternative": "Generated article summary"
}
```

---

# Implementation details

## FeedEntryNoteREST

Responsible for exposing note-related REST endpoints:

* `POST /rest/note`
* `GET /rest/note`
* `GET /rest/note/{id}`

The resource validates requests, obtains the authenticated user and delegates business logic to `FeedEntryNoteService`.

## FeedEntryNoteService

Responsible for note business logic:

* Validates that the feed entry exists
* Creates `FeedEntryNote`
* Retrieves notes by user
* Retrieves a specific note owned by the user

---

## FeedEntryAlternativeService

Responsible for AI-powered content generation:

* Finds the feed entry
* Extracts title or content depending on requested target
* Calls the LLM client
* Builds the response containing original and generated content

---

## Database

Added database migration for storing feed entry notes.

The note entity stores:

* User reference
* Feed entry reference
* Comment
* Rating

Database migrations are executed automatically on application startup using Flyway.
