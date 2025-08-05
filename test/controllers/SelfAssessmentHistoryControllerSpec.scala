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
import models.RequestData
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsEmpty, ControllerComponents, Result}
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import services.SelfAssessmentService
import shared.SpecBase
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.{ExecutionContext, Future}

class SelfAssessmentHistoryControllerSpec
    extends SpecBase{
  def createBypassAuthAction(): AuthenticateRequestAction = {
    val mockAuth = mock[AuthenticateRequestAction]

    when(mockAuth.apply(any[String])).thenAnswer { invocation =>
      val utr = invocation.getArgument[String](0)

      new ActionBuilder[RequestData, AnyContent] {
        override def parser = play.api.mvc.BodyParsers.Default.anyContent
        override protected def executionContext = ExecutionContext.global

        override def invokeBlock[A](request: play.api.mvc.Request[A], block: RequestData[A] => Future[Result]): Future[Result] = {
          val requestData = RequestData(utr, None, request)
          block(requestData)
        }
      }
    }
    mockAuth
  }

  "SelfAssessmentHistoryController" should {
    "return OK with success message" in {

      val application = applicationBuilder()
        .overrides(bind[AuthenticateRequestAction].toInstance(createBypassAuthAction()))
        .build()

      running(application) {
        val controller = application.injector.instanceOf[SelfAssessmentHistoryController]
        val result = controller.getYourSelfAssessmentData(testUtr)(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.obj("message" -> "Success!")
      }
    }
  }
}