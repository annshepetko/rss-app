# Feed Entry Notes & AI Alternative Generation

## Implemented Functionality

### Feed Entry Notes

Added functionality for creating and retrieving personal notes attached to feed entries.

Implemented features:

- Create a note for a feed entry
- Retrieve all notes belonging to the authenticated user
- Retrieve a specific note by ID
- User-level authorization

Each note contains:

- User reference
- Feed entry reference
- Comment
- Rating

Only authenticated users with the `USER` role can access the note endpoints.

---

### AI Alternative Generation

Added an endpoint that generates an alternative version of a feed entry title or content using an LLM.

The endpoint:

- Loads the requested feed entry
- Extracts the selected target (`TITLE` or `CONTENT`)
- Sends the original text together with the user's prompt to the LLM
- Returns both the original content and the generated alternative

---

# Getting Started

## 1. Configure the Groq API key

Before starting the application, configure the required environment variable.

In IntelliJ IDEA:

1. Open **Run → Edit Configurations...**
2. Select the application run configuration.
3. In the **Environment variables** field, add:

```
GROQ_API_KEY=<your_groq_api_key>
```

4. Apply the changes and save the configuration.

> **Note**
>
> A valid Groq API key is required for the AI Alternative Generation endpoint.

---

## 2. Start the application

Run the application normally.

---

## 3. Create the initial user

The application requires an initial user before importing the demo data.

Send the following request:

```http
POST http://localhost:8083/rest/user/initialSetup
Content-Type: application/json
```

```json
{
  "name": "tesddatuassqwerqser",
  "password": "Passqeword123!",
  "email": "teaeisddasajorgqewjrg9studser@example.com"
}
```

---

## 4. Configure Postman authentication

All protected endpoints require **HTTP Basic Authentication**.

Configure Postman:

- Authorization → **Basic Auth**

Username:

```
tesddatuassqwerqser
```

Password:

```
Passqeword123!
```

These credentials should be used for every authenticated request.

---

## 5. Stop the application

After the initial user has been created, stop the application.

---

## 6. Import demo data

The demo data script is located at:

```
src/main/resources/data/demo-data.sql
```

### Importing the script into H2

1. Open **View → Tool Windows → Database**.
2. Click **+** → **Data Source** → **H2**.
3. Configure the connection using the values from `application.properties`:
   - JDBC URL
   - Username
   - Password
4. Test the connection.
5. Create a new SQL Console.
6. Open:

```
src/main/resources/data/demo-data.sql
```

7. Execute the script.

The script inserts sample:

- Feed
- Feed Category
- Feed Subscription
- Feed Entry
- Feed Entry Content
- Feed Entry Status
- Feed Entry Tag

These records are required for testing the implemented functionality.

---

## 7. Restart the application

Start the application again.

The application is now ready for testing.

The demo data creates a sample feed entry with **ID = 1**, which is used in the examples below.

---

# Testing the API

The examples below assume that:

- the application is running on `http://localhost:8083`
- HTTP Basic Authentication is configured
- the demo data has been imported successfully

---

# Feed Entry Notes API

## Create a note

Creates a personal note for a feed entry.

### Request

```http
POST /rest/note
Content-Type: application/json
```

```json
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

## Get all notes

Returns all notes created by the authenticated user.

### Request

```http
GET /rest/note
```

### Example Response

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

## Get note by ID

Returns a single note belonging to the authenticated user.

### Request

```http
GET /rest/note/1
```

### Example Response

```json
{
  "id": 1,
  "entryId": 1,
  "comment": "Interesting article about Java",
  "rating": 5
}
```

If the note does not exist or does not belong to the authenticated user:

```http
404 Not Found
```

---

# AI Alternative Generation API

## Generate an alternative title

Generates an alternative version of the feed entry title.

### Request

```http
POST /rest/entry/1/generate-alternative
Content-Type: application/json
```

```json
{
  "target": "TITLE",
  "prompt": "Make the title shorter and more engaging"
}
```

### Example Response

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

## Generate alternative content

Generates an alternative version of the feed entry content.

### Request

```http
POST /rest/entry/1/generate-alternative
Content-Type: application/json
```

```json
{
  "target": "CONTENT",
  "prompt": "Summarize this article in a clear way"
}
```

### Example Response

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

# Authorization

All endpoints described in this document require **HTTP Basic Authentication**.

Use the credentials created during the **Initial Setup** step.

Username:

```
tesddatuassqwerqser
```

Password:

```
Passqeword123!
```

The authenticated user must have the `USER` role.

---

# Database

Added a Flyway migration for storing feed entry notes.

The `FeedEntryNote` entity stores:

- User reference
- Feed entry reference
- Comment
- Rating

Database migrations are executed automatically on application startup using Flyway.