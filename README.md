
# self-assessment-liability-api

This is a placeholder README.md for a new repository

* Project structure?
* Request/Response see: https://github.com/hmrc/citizen-details?tab=readme-ov-file#get-citizen-detailsidnametaxid and https://github.com/hmrc/mtd-identifier-lookup
** Field detail and explanation
* Starting the application
* Running tests
* Stub info
* Link to the service guide

More information about this service can be found in the [service guide](https://developer.service.hmrc.gov.uk/guides/self-assessment-liability-service-guide). 

## Endpoint Definition

### Request

Method: `GET`

URL: `/:utr`

Query Parameter: `fromDate`

### Example request URLs:
* `/1097172987`
* `/1097172987?2025-01-01`

### Response Status Codes

More detail can be found in the [service guide error responses](https://developer.service.hmrc.gov.uk/guides/self-assessment-liability-service-guide/documentation/errors-responses.html).

| Status Code | Description                                                                                                                                                                                                                                         |
|-------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 200         | Successful request.                                                                                                                                                                                                                                 |
| 400         | Invalid API implementation caused by (at least) one of: <br/> * Invalid `fromDate` - either malformed or outside of the accepted range (the accepted range is 7 tax years ago to now). <br/> * Invalid `utr`. <br/> * Missing authentication token. |
| 401         | Unauthorized.                                                                                                                                                                                                                                       |
| 403         | Forbidden.                                                                                                                                                                                                                                          |
| 404         | Not Found.                                                                                                                                                                                                                                          |
| 500         | Internal Server Error.                                                                                                                                                                                                                              |
| 503         | Service Unavailable.                                                                                                                                                                                                                                |

### Example success response body

```json
{
  "balanceDetails": {
    "totalOverdueBalance": 500,
    "totalPayableBalance": 500,
    "payableDueDate": "2025-04-31",
    "totalPendingBalance": 1500,
    "pendingDueDate": "2025-07-15",
    "totalBalance": 2000,
    "totalCodedOut": 250,
    "totalCreditAvailable": 0
  },
  "chargeDetails": [
    {
      "chargeId": "AB1234567",
      "creationDate": "2025-01-15",
      "chargeType": "ITSA",
      "chargeAmount": 1250,
      "outstandingAmount": 500,
      "taxYear": "2024-2025",
      "dueDate": "2025-04-31",
      "amendments": {
        "amendmentDate": "2025-04-15",
        "amendmentAmount": 500,
        "newChargeBalance": 750,
        "paymentMethod": "bank_transfer",
        "paymentDate": "2025-04-10"
      },
      "codedOutDetail": [
        {
          "amount": 250,
          "effectiveDate": "2025-04-01",
          "taxYear": 2024,
          "effectiveTaxYear": 2025
        }
      ]
    },
    {
      "chargeId": "EF2345678",
      "creationDate": "2024-02-10",
      "chargeType": "NICS",
      "chargeAmount": 2200,
      "outstandingAmount": 0,
      "taxYear": 2023,
      "dueDate": "2024-04-01",
      "interestAmountDue": "20.00",
      "accruingInterestDateRange": {
        "interestStartDate": "2024-05-01",
        "interestEndDate": "2024-12-01"
      },
      "interestRate": 0.05,
      "amendments": {
        "newChargeBalance": 2000,
        "amendmentDate": "2024-12-08",
        "amendmentAmount": 2058.33,
        "paymentMethod": "bank_transfer",
        "paymentDate": "2024-12-03"
      }
    },
    {
      "chargeId": "KL3456789",
      "creationDate": "2025-05-22",
      "chargeType": "VATC",
      "chargeAmount": 1500,
      "outstandingAmount": 1500,
      "taxYear": 2025,
      "dueDate": "2025-07-15"
    }
  ],
  "refundDetails": [
    {
      "issueDate": "2024-01-10",
      "refundMethod": "bank_transfer",
      "refundRequestDate": "2023-12-12",
      "refundRequestAmount": 350,
      "refundDescription": "From overpayment from return 05 APR 23",
      "interestAddedToRefund": 5.25,
      "refundActualAmount": 355.25,
      "refundStatus": "processed"
    }
  ],
  "paymentHistoryDetails": [
    {
      "paymentAmount": 500,
      "paymentId": "PAY123456",
      "paymentMethod": "bank_transfer",
      "paymentDate": "2025-04-11",
      "dateProcessed": "2025-04-15",
      "allocationReference": "AB1234567"
    },
    {
      "paymentAmount": 2058.33,
      "paymentId": "PAY112233",
      "paymentMethod": "bank_transfer",
      "paymentDate": "2024-12-04",
      "dateProcessed": "2024-12-08",
      "allocationReference": "EF2345678"
    },
    {
      "paymentAmount": 200,
      "paymentId": "PAY888233",
      "paymentMethod": "bank_transfer",
      "paymentDate": "2023-12-04",
      "dateProcessed": "2023-12-08",
      "allocationReference": "EF2345678"
    }
  ]
}
```

### Example failure response body

```json
{
  "failure": {
    "type": "BAD_REQUEST",
    "reason": "Invalid request format or parameters."
  }
}
```

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

