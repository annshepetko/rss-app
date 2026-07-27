# Getting Started

## Preparing demo data

To test the implemented functionality, a demo user and demo feed data are required.

### 1. Start the application

Run the application normally.

---

### 2. Create the initial user

Send the following request:

```http
POST http://localhost:8083/rest/user/initialSetup
Content-Type: application/json

{
  "name": "tesddatuassqwerqser",
  "password": "Passqeword123!",
  "email": "teaeisddasajorgqewjrg9studser@example.com"
}
```

This creates the user required for the demo data script.

---

### 3. Configure authentication in Postman

All protected endpoints use **HTTP Basic Authentication**.

Configure Postman:

- Authorization → **Basic Auth**
- Username:

```
tesddatuassqwerqser
```

- Password:

```
Passqeword123!
```

These credentials should be used for all authenticated requests.

---

### 4. Stop the application

After the user has been created, stop the application.

---

### 5. Execute the demo data script

The demo data script is located at:

```
src/main/resources/data/demo-data.sql
```

The script inserts sample feeds, subscriptions and feed entries required for testing.

#### Connecting to the H2 database from IntelliJ IDEA

1. Open **View → Tool Windows → Database**.
2. Click **+** → **Data Source** → **H2**.
3. Configure the connection using the same values defined in `application.properties`:

- JDBC URL
- Username
- Password

4. Test the connection.
5. Open the SQL Console.
6. Execute:

```
src/main/resources/data/demo-data.sql
```

---

### 6. Start the application again

Restart the application.

The API is now ready for testing.

The demo data contains sample feed entries with IDs referenced in the examples below.
# Testing the API

After completing the setup described above, the application is ready for testing.

The examples below assume:

- the application is running on `http://localhost:8083`
- HTTP Basic Authentication is configured in Postman
- the demo data has been imported successfully

The demo data creates a sample feed entry with **ID = 1**, which is used in the following requests.

---

# Feed Entry Notes

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

## Get note by ID

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

If the note does not exist or belongs to another user:

```http
404 Not Found
```

---

# AI Alternative Generation

The application also provides an endpoint for generating an alternative title or content for a feed entry using an LLM.

The endpoint:

- loads the requested feed entry
- extracts either its title or content
- sends the selected text together with the user prompt to the LLM
- returns both the original text and the generated alternative

---

## Generate an alternative title

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

## Generate alternative content

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

### Response

```json
{
  "original": {
    "id": "1",
    "title": "Original article title",
    "content": "Original article content"
  },
  "target": "CONTENT",
  "prompt": "Summarize this article in a clear way"
,
  "generatedAlternative": "Generated article summary"
}
```

---

# Authorization

All endpoints described above require authentication using **HTTP Basic Authentication**.

Use the credentials created during the initial setup:

Username:

```
tesddatuassqwerqser
```

Password:

```
Passqeword123!
```

The authenticated user must have the `USER` role.