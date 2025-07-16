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
import models.ApiErrorResponses
import models.StandardErrorResponses.internalServerError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.SelfAssessmentService
import shared.{HttpWireMock, SpecBase}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved

import scala.concurrent.Future

class SelfAssessmentHistoryControllerSpec extends SpecBase with HttpWireMock {
  override lazy val app: Application = new GuiceApplicationBuilder().build()

  private val authConnector: AuthConnector = mock[AuthConnector]
  private val selfAssessmentService: SelfAssessmentService = mock[SelfAssessmentService]
  private val appConfig: AppConfig = mock[AppConfig]
  private val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]
  private val utr: String = "1234567890"
  private val date: Option[String] = Some("2025-04-06")
  private val controller =
    new SelfAssessmentHistoryController(
      authConnector,
      selfAssessmentService,
      cc
    )(
      appConfig,
      ec
    )
  private val jsonResponse: String = """
    |{
    |  "someJson": []
    |}""".stripMargin

  private def controllerMethod(
      utr: String,
      fromDate: Option[String],
      controller: SelfAssessmentHistoryController
  ): Action[AnyContent] = controller.getYourSelfAssessmentData(utr, fromDate)

  "SelfAssessmentHistoryControllerSpec" when {
    when(authConnector.authorise(any(), any())(any(), any()))
      .thenReturn(Future.successful(Some(Individual) and ConfidenceLevel.L250))

    "getting self assessment data" should {
      "return details as JSON when successful" in {
        when(selfAssessmentService.getHipData(any(), any())(any()))
          .thenReturn(Future.successful(Ok(jsonResponse)))

        running(app) {
          val result = controllerMethod(utr, date, controller)(FakeRequest())

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.parse(jsonResponse)
        }
      }

      "return an error when the request fails" in {
        when(selfAssessmentService.getHipData(any(), any())(any()))
          .thenReturn(internalServerError)

        running(app) {
          val result = controllerMethod(utr, date, controller)(FakeRequest())

          status(result) mustBe INTERNAL_SERVER_ERROR
          contentAsJson(result) mustBe ApiErrorResponses(
            "Internal Server Error",
            "Unexpected internal error. Please try again later."
          ).asJson
        }
      }
    }
  }
}
