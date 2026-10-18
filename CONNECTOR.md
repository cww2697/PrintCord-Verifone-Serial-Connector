# Print Connector Implementation Guide

Print Connectors are small agents that bridge the PrintCord bot and a physical receipt printer.

## Base URL
All API requests should be made to the following base URL:
`http://printcord.canyonwolf.net/api/connectors`

## Registration Flow
1. User requests a registration code via `/register` on Discord.
2. The bot provides an 8-character code (format: `XXXX-XXXX`) which is **valid for 15 minutes**.
3. Connector prompts user for the code and a name for the printer.
4. Connector sends a registration request:
   ```http
   POST /api/connectors/register
   Content-Type: application/json

   {
     "code": "1234-5678",
     "name": "My Thermal Printer"
   }
   ```
5. Bot responds with:
   ```json
   {
     "connectorId": "uuid-string",
     "apiToken": "secret-auth-token"
   }
   ```
6. Connector stores the `apiToken` securely.

## Message Polling
Connectors should poll the bot periodically (e.g., every 5-10 seconds) for new print jobs.

```http
GET /api/connectors/poll
Authorization: Bearer <apiToken>
```

**Response:** A JSON array of message objects.
```json
[
  {
    "content": "Hello from Discord!",
    "sender": "Cody",
    "timestamp": "2024-03-20T12:00:00Z"
  },
  {
    "content": "Another message to print.",
    "sender": "Alice",
    "timestamp": "2024-03-20T12:05:00Z"
  }
]
```
- `content`: The raw text message to print.
- `sender`: The Discord username of the person who sent the message.
- `timestamp`: ISO-8601 formatted time when the message was sent.

## Heartbeat / Active Status
Polling automatically updates the connector's "last active" timestamp. If a connector does not poll for **5 minutes**, it is considered inactive and will not receive new messages.

Alternatively, a heartbeat can be sent without polling for messages:
```http
POST /api/connectors/status
Authorization: Bearer <apiToken>
```

## Deregistration

### User-Initiated (Discord)
Users can deregister a connector via the `/deregister` slash command on Discord. Once deregistered, the `apiToken` will become invalid.

### Connector-Initiated (API)
A connector can deregister itself (e.g., during a factory reset):
```http
DELETE /api/connectors
Authorization: Bearer <apiToken>
```

### Handling Deregistration
If a connector receives a `401 Unauthorized` response to a poll or status request, it should assume it has been deregistered, stop its polling loop, and potentially prompt for a new registration.
