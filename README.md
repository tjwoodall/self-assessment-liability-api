
# self-assessment-liability-api

This is a placeholder README.md for a new repository

* Project structure?
* Request/Response see: https://github.com/hmrc/citizen-details?tab=readme-ov-file#get-citizen-detailsidnametaxid and https://github.com/hmrc/mtd-identifier-lookup
** Field detail and explanation
* Starting the application
* Running tests
* Stub info
* Link to the service guide

## Endpoint Definition (API)

### Request

Method: `GET`

URL: `/:utr`

Query Parameters: `fromDate`

### Example request URLs:
* `/1097172987`
* `/1097172987?2025-01-01`

### Response Status Codes

| StatusCode | Description |
|------------|-------------|
| 200        | TODO.       |
| 400        | TODO.       |
| 404        | TODO.       |
| 500        | TODO.       |

### Example success response body

```json
{
  "success": "todo"
}
```

### Example failure response body

```json
{
  "failure": "todo"
}
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

