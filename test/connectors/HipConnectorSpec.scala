/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import models.ServiceErrors.*
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import shared.{HttpWireMock, SpecBase}

class HipConnectorSpec extends SpecBase with HttpWireMock {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "microservice.services.hip.port" -> server.port()
    )
    .build()
  private lazy val connector: HipConnector =
    app.injector.instanceOf[HipConnector]
  private val utr: String = "1234567890"
  private val dateFrom: String = "2025-04-06"
  private val serviceUrl = s"/self-assessment/account/$utr/liability-details?dateFrom=$dateFrom"
  private val successResponse: String = """{
  "balanceDetails": {
    "totalOverdueBalance": 500.00,
    "totalPayableBalance": 500.00,
    "payableDueDate": "2025-04-31",
    "totalPendingBalance": 1500.00,
    "pendingDueDate": "2025-07-15",
    "totalBalance": 2000.00,
    "totalCodedOut": 250.00,
    "totalCreditAvailable": 0.00
  },
  "chargeDetails": [
    {
      "chargeId": "KL3456789",
      "creationDate": "2025-05-22",
      "chargeType": "VATC",
      "chargeAmount": 1500.00,
      "outstandingAmount": 1500.00,
      "taxYear": "2025-2026",
      "dueDate": "2025-07-15"
    }
  ],
  "refundDetails": [],
  "paymentHistoryDetails": [
    {
      "paymentAmount": 500.00 ,
      "paymentReference": "PAY123456",
      "paymentMethod": "bank_transfer",
      "paymentDate": "2025-04-11"
      "dateProcessed": "2025-04-15",
      "allocationReference": "AB1234567",
    }
  ]
}"""

  "getSelfAssessmentData" should {
    "return JSON associated with the utr and date if 200 response is received" in {
      simmulateGet(serviceUrl, OK, successResponse)
      val result = connector.getSelfAssessmentData(utr, dateFrom)
      result.futureValue mustBe Json.toJson(successResponse)
    }

    "return expected error if returned JSON is malformed" in {
      simmulateGet(serviceUrl, OK, """{malformedJson}""")
      val result = connector.getSelfAssessmentData(utr, dateFrom)
      result.failed.futureValue mustBe Downstream_Error
    }

    "return expected error if 400 response is received" in {
      simmulateGet(serviceUrl, BAD_REQUEST, "")
      val result = connector.getSelfAssessmentData(utr, dateFrom)
      result.failed.futureValue mustBe Invalid_Correlation_Id
    }

    "return expected error if 401 response is received" in {
      simmulateGet(serviceUrl, UNAUTHORIZED, "")
      val result = connector.getSelfAssessmentData(utr, dateFrom)
      result.failed.futureValue mustBe HIP_Unauthorised
    }

    "return expected error if 403 response is received" in {
      simmulateGet(serviceUrl, FORBIDDEN, "")
      val result = connector.getSelfAssessmentData(utr, dateFrom)
      result.failed.futureValue mustBe HIP_Forbidden
    }

    "return expected error if 404 response is received" in {
      simmulateGet(serviceUrl, NOT_FOUND, "")
      val result = connector.getSelfAssessmentData(utr, dateFrom)
      result.failed.futureValue mustBe No_Payments_Found_For_UTR
    }

    "return expected error if 422 response is received" in {
      simmulateGet(serviceUrl, UNPROCESSABLE_ENTITY, "")
      val result = connector.getSelfAssessmentData(utr, dateFrom)
      result.failed.futureValue mustBe Invalid_UTR
    }

    "return expected error if 500 response is received" in {
      simmulateGet(serviceUrl, INTERNAL_SERVER_ERROR, "")
      val result = connector.getSelfAssessmentData(utr, dateFrom)
      result.failed.futureValue mustBe HIP_Server_Error
    }

    "return expected error if 502 response is received" in {
      simmulateGet(serviceUrl, BAD_GATEWAY, "")
      val result = connector.getSelfAssessmentData(utr, dateFrom)
      result.failed.futureValue mustBe HIP_Bad_Gateway
    }

    "return expected error if 503 response is received" in {
      simmulateGet(serviceUrl, SERVICE_UNAVAILABLE, "")
      val result = connector.getSelfAssessmentData(utr, dateFrom)
      result.failed.futureValue mustBe HIP_Service_Unavailable
    }

    "return Downstream_Error in case of any other response" in {
      simmulateGet(serviceUrl, IM_A_TEAPOT, "")
      val result = connector.getSelfAssessmentData(utr, dateFrom)
      result.failed.futureValue mustBe Downstream_Error
    }
  }
}
