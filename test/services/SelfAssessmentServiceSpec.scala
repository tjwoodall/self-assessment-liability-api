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

package services

import connectors.{CitizenDetailsConnector, HipConnector, MtdIdentifierLookupConnector}
import models.{ApiErrorResponses, HipResponse}
import models.ServiceErrors.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.prop.TableDrivenPropertyChecks.forEvery
import org.scalatest.prop.Tables.Table
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers.*
import shared.{HttpWireMock, SpecBase}

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
            "Authorisation failed"
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
            "Service unavailable. Pleased try again later."
          ).asJson
        }
      }
    }
  }
}
