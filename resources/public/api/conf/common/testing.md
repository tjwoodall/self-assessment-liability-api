API testing is performed in the [HMRC developer hub](https://developer.service.hmrc.gov.uk/api-documentation) “sandbox” environment. Once you’ve [registered for an account](https://developer.service.hmrc.gov.uk/developer/registration) you can conduct your own testing. The *View Self Assessment Account* API belongs to the "Self Assessment" category.

Work through the instructions on the [getting started](https://developer.service.hmrc.gov.uk/api-documentation/docs/using-the-hub) page to create an application, then locate and subscribe to the following APIs:
* **Create Test User**
* **View Self Assessment Account**

Before sending a GET request to the *View Self Assessment Account* API:
- [Create a test user](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/api-platform-test-user/1.0) with a *View Self Assessment Account* subscription.
- Create a bearer token for the test user.

A GET request can then be sent using:
- A (required) UTR. The UTR can be obtained when creating a test user via the "saUtr" value in a successful response.
- An (optional) fromDate. The fromDate has the form YYYY-MM-DD. Omitting the fromDate will use the default time period of the current and previous tax years.
