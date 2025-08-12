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

import connectors.HipConnectorSpec.{hipResponse, jsonSuccessResponse}
import models.HipResponse
import models.ServiceErrors.*
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import shared.{HttpWireMock, SpecBase}

import java.time.LocalDate

class HipConnectorSpec extends SpecBase with HttpWireMock {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "microservice.services.hip.port" -> server.port()
    )
    .build()
  private lazy val connector: HipConnector =
    app.injector.instanceOf[HipConnector]
  private val utr: String = "1234567890"
  private val fromDate: String = "2025-04-06"
  private val toDate: String = LocalDate.now.toString
  private val serviceUrl =
    s"/self-assessment/account/$utr/liability-details?dateFrom=$fromDate&dateTo=$toDate"

  "getSelfAssessmentData" should {
    "return JSON associated with the utr and date if 200 response is received" in {
      simulateGet(serviceUrl, OK, jsonSuccessResponse)
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.futureValue mustBe hipResponse
    }

    "return expected error if returned JSON is malformed" in {
      simulateGet(serviceUrl, OK, """{malformedJson}""")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe Downstream_Error
    }

    "return expected error if 400 response is received" in {
      simulateGet(serviceUrl, BAD_REQUEST, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe Downstream_Error
    }

    "return expected error if 401 response is received" in {
      simulateGet(serviceUrl, UNAUTHORIZED, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe Downstream_Error
    }

    "return expected error if 403 response is received" in {
      simulateGet(serviceUrl, FORBIDDEN, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe Downstream_Error
    }

    "return expected error if 404 response is received" in {
      simulateGet(serviceUrl, NOT_FOUND, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe No_Data_Found
    }

    "return expected error if 422 response is received" in {
      simulateGet(serviceUrl, UNPROCESSABLE_ENTITY, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe Downstream_Error
    }

    "return expected error if 500 response is received" in {
      simulateGet(serviceUrl, INTERNAL_SERVER_ERROR, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe Downstream_Error
    }

    "return expected error if 502 response is received" in {
      simulateGet(serviceUrl, BAD_GATEWAY, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe Downstream_Error
    }

    "return expected error if 503 response is received" in {
      simulateGet(serviceUrl, SERVICE_UNAVAILABLE, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe Downstream_Error
    }

    "return Downstream_Error in case of any other response" in {
      simulateGet(serviceUrl, IM_A_TEAPOT, "")
      val result = connector.getSelfAssessmentData(utr, fromDate, toDate)
      result.failed.futureValue mustBe Downstream_Error
    }
  }
}


object HipConnectorSpec {
  val jsonSuccessResponse: String =
    """
      |{
      |  "balanceDetails": {
      |  "totalOverdueBalance": 500.00,
      |  "totalPayableBalance": 500.00,
      |  "payableDueDate": "2025-04-31",
      |  "totalPendingBalance": 1500.00,
      |  "pendingDueDate": "2025-07-15",
      |  "totalBalance": 2000.00,
      |  "totalCreditAvailable": 0.00,
      |    "codedOutDetail": [
      |      {
      |        "totalAmount": 100.00,
      |        "effectiveStartDate": "2021-12-04",
      |        "effectiveEndDate": "2023-10-04"
      |      }
      |    ]
      |},
      |"chargeDetails": [
      |  {
      |    "chargeId": "KL3456789",
      |    "creationDate": "2025-05-22",
      |    "chargeType": "VATC",
      |    "chargeAmount": 1500.00,
      |    "outstandingAmount": 1500.00,
      |    "taxYear": "2025-2026",
      |    "dueDate": "2025-07-15",
      |    "amendments": [
      |      {
      |        "amendmentDate": "ad",
      |        "amendmentAmount": 0.00,
      |        "amendmentReason": "ar"
      |      }
      |    ]
      |  }
      |],
      |"refundDetails": [
      |  {
      |    "issueDate": "id",
      |    "refundMethod": "rm",
      |    "refundRequestDate": "rrd",
      |    "refundRequestAmount": 0.00,
      |    "refundReference": "rr",
      |    "interestAddedToRefund": 0.00,
      |    "refundActualAmount": 0.00,
      |    "refundStatus": "rs"
      |  }
      |],
      |"paymentHistoryDetails": [
      |  {
      |    "paymentAmount": 500.00,
      |    "paymentId": "payment reference id",
      |    "paymentMethod": "payment method",
      |    "paymentDate": "2025-04-11",
      |    "dateProcessed": "2025-04-15",
      |    "allocationReference": "allocation reference"
      |  }
      |]
      |}""".stripMargin
  val hipResponse: HipResponse = Json.parse(jsonSuccessResponse).as[HipResponse]
}
