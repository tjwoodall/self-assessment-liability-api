/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers

import config.AppConfig
import connectors.{CitizenDetailsConnector, HipConnector, MtdIdentifierLookupConnector}
import models.{ApiErrorResponses, HipResponse}
import models.ServiceErrors.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.prop.TableDrivenPropertyChecks.forEvery
import org.scalatest.prop.Tables.Table
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.{Application, UnexpectedException}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.SelfAssessmentService
import shared.{HttpWireMock, SpecBase}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import utils.FutureConverter.FutureOps

import scala.concurrent.Future

class SelfAssessmentServiceSpec extends SpecBase with HttpWireMock {
  override lazy val app: Application = new GuiceApplicationBuilder().build()

  private val cidConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  private val mtdConnector: MtdIdentifierLookupConnector = mock[MtdIdentifierLookupConnector]
  private val hipConnector: HipConnector = mock[HipConnector]
  private val selfAssessmentService: SelfAssessmentService = new SelfAssessmentService(
    cidConnector,
    mtdConnector,
    hipConnector
  )(
    ec
  )
  private val utr: String = "1234567890"
  private val date: String = "2025-04-06"
  private val jsonResponse: String =
   """
      |{
      |  "balanceDetails": {
      |  "totalOverdueBalance": 500.00,
      |  "totalPayableBalance": 500.00,
      |  "payableDueDate": "2025-04-31",
      |  "totalPendingBalance": 1500.00,
      |  "pendingDueDate": "2025-07-15",
      |  "totalBalance": 2000.00,
      |  "totalCodedOut": 250.00,
      |  "totalCreditAvailable": 0.00
      |},
      |"chargeDetails": [
      |  {
      |    "chargeId": "KL3456789",
      |   "creationDate": "2025-05-22",
      |    "chargeType": "VATC",
      |    "chargeAmount": 1500.00,
      |    "outstandingAmount": 1500.00,
      |    "taxYear": "2025-2026",
      |    "dueDate": "2025-07-15",
      |    "amendments": [
      |      {
      |        "amendmentId": "aid",
      |        "amendmentType": "at",
      |        "amendmentDate": "ad",
      |        "amendmentAmount": 0.00
      |      }
      |    ],
      |    "codedOutDetail": [
      |      {
      |        "amount": 0.00,
      |        "codedChargeType": "cdt",
      |        "effectiveDate": "ed",
      |        "taxYear": "ty",
      |        "effectiveTaxYear": "ety"
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
      |    "paymentAmount": 500.00 ,
      |    "paymentDate": "2025-04-11",
      |    "dateProcessed": "2025-04-15"
      |  }
      |]
    |}""".stripMargin
  private val hipResponse: HipResponse = Json.parse(jsonResponse).as[HipResponse]

  "SelfAssessmentServiceSpec" when {
    "getting HIP data" should {
      "return details as JSON when successful" in {
        when(hipConnector.getSelfAssessmentData(any(), any())(any(), any()))
          .thenReturn(Future.successful(hipResponse))

        running(app) {
          val result = selfAssessmentService.getHipData(utr, date)

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(hipResponse)
        }
      }

      "return Bad Request for relevant HIP error(s)" in {
        when(hipConnector.getSelfAssessmentData(any(), any())(any(), any()))
          .thenReturn(Future.failed(No_Payments_Found_For_UTR))

        running(app) {
          val result = selfAssessmentService.getHipData(utr, date)

          status(result) mustBe BAD_REQUEST
          contentAsJson(result) mustBe ApiErrorResponses(
            "Bad Request",
            "Invalid request format or parameters"
          ).asJson
        }
      }

      "return Unauthorized for relevant HIP error(s)" in {
        when(hipConnector.getSelfAssessmentData(any(), any())(any(), any()))
          .thenReturn(Future.failed(HIP_Unauthorised))

        running(app) {
          val result = selfAssessmentService.getHipData(utr, date)

          status(result) mustBe UNAUTHORIZED
          contentAsJson(result) mustBe ApiErrorResponses(
            "Unauthorised",
            "Invalid request format or parameters"
          ).asJson
        }
      }

      "return Forbidden for relevant HIP error(s)" in {
        when(hipConnector.getSelfAssessmentData(any(), any())(any(), any()))
          .thenReturn(Future.failed(HIP_Forbidden))

        running(app) {
          val result = selfAssessmentService.getHipData(utr, date)

          status(result) mustBe FORBIDDEN
          contentAsJson(result) mustBe ApiErrorResponses(
            "Forbidden",
            "Access not permitted"
          ).asJson
        }
      }

      "return Internal Server Error for relevant HIP error(s)" in {
        forEvery(
          Table(
            "Internal Server Errors",
            Invalid_Correlation_Id,
            Invalid_UTR,
            HIP_Server_Error,
            HIP_Bad_Gateway,
            Downstream_Error,
            Throwable()
          )
        ) { serviceError =>
          when(hipConnector.getSelfAssessmentData(any(), any())(any(), any()))
            .thenReturn(Future.failed(serviceError))

          running(app) {
          val result = selfAssessmentService.getHipData(utr, date)

            status(result) mustBe INTERNAL_SERVER_ERROR
            contentAsJson(result) mustBe ApiErrorResponses(
              "Internal Server Error",
              "Unexpected internal error. Please try again later."
            ).asJson
          }
        }
      }

      "return Service Unavailable for relevant HIP error(s)" in {
        when(hipConnector.getSelfAssessmentData(any(), any())(any(), any()))
          .thenReturn(Future.failed(HIP_Service_Unavailable))

        running(app) {
          val result = selfAssessmentService.getHipData(utr, date)

          status(result) mustBe SERVICE_UNAVAILABLE
          contentAsJson(result) mustBe ApiErrorResponses(
            "Service Unavailable",
            "Service unavailable. Pleased try again later"
          ).asJson
        }
      }
    }
  }
}
