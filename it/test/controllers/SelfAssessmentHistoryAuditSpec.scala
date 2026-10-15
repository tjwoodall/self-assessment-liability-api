/*
 * Copyright 2026 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, post, postRequestedFor, urlEqualTo, urlPathEqualTo}
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.Eventually
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel, InsufficientEnrolments}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import utils.IntegrationSpecBase

import scala.concurrent.{ExecutionContext, Future}

class SelfAssessmentHistoryAuditSpec
  extends IntegrationSpecBase
    with Eventually {

  override lazy val app: Application = {
    server.start()

    new GuiceApplicationBuilder()
      .overrides(
        play.api.inject.bind[AuthConnector]
          .toInstance(mockAuthConnector)
      )
      .configure(
        "play.http.errorHandler" ->
          "controllers.GlobalErrorHandler",
        "microservice.services.citizen-details.port" ->
          server.port(),
        "microservice.services.mtd-id-lookup.port" ->
          server.port(),
        "microservice.services.hip.port" ->
          server.port(),
        "auditing.enabled" -> true,
        "auditing.consumer.baseUri.host" -> "localhost",
        "auditing.consumer.baseUri.port" -> server.port(),
        "metrics.enabled" -> false
      )
      .build()
  }

  private def stubAuditEndpoint(): Unit = {
    server.resetAll()

    server.stubFor(
      post(urlEqualTo("/write/audit"))
        .willReturn(
          aResponse().withStatus(NO_CONTENT)
        )
    )

    server.stubFor(
      post(urlEqualTo("/write/audit/merged"))
        .willReturn(
          aResponse().withStatus(NO_CONTENT)
        )
    )
  }

  private def verifyRequestReceivedAudit(
                                          expectedStatus: Int
                                        ): Unit = {

    eventually {
      val auditRequests =
        server.findAll(
          postRequestedFor(
            urlEqualTo("/write/audit")
          )
        )

      val requestReceivedAudits: Seq[JsValue] =
        auditRequests
          .toArray
          .map(_.asInstanceOf[LoggedRequest])
          .map(request =>
            Json.parse(request.getBodyAsString)
          )
          .filter { auditBody =>
            (auditBody \ "auditType")
              .asOpt[String]
              .contains("RequestReceived")
          }
          .toSeq

      val matchingAudit =
        requestReceivedAudits.find { auditBody =>
          (auditBody \ "detail" \ "statusCode")
            .asOpt[String]
            .contains(expectedStatus.toString)
        }.getOrElse {
          fail(
            s"No RequestReceived audit event with status $expectedStatus was found"
          )
        }

      val detail = matchingAudit \ "detail"

      (detail \ "statusCode")
        .asOpt[String] mustBe Some(
        expectedStatus.toString
      )

      (detail \ "responseMessage")
        .asOpt[String] must not be empty

      (detail \ "failedRequestReason")
        .toOption mustBe None
    }
  }

  "SelfAssessmentHistoryController request auditing" should {

    "audit statusCode and responseMessage for a successful request" in {
      stubAuditEndpoint()

      when(
        mockAuthConnector.authorise[RetrievalsType](
          any[Predicate](),
          any[Retrieval[RetrievalsType]]()
        )(any[HeaderCarrier](), any[ExecutionContext]())
      ).thenAnswer { invocation =>
        val retrieval = invocation.getArgument[Retrieval[?]](1)

        if (retrieval == (affinityGroup and confidenceLevel)) {
          Future.successful(
            new~(Some(Individual), ConfidenceLevel.L250)
          )
        } else {
          Future.successful(())
        }
      }

      val successfulHipResponse = Json.obj(
        "balanceDetails" -> Json.obj(
          "totalOverdueBalance" -> 0,
          "totalPayableBalance" -> 0,
          "totalPendingBalance" -> 0,
          "totalBalance" -> 0,
          "totalCreditAvailable" -> 0
        )
      )

      server.stubFor(
        get(urlPathEqualTo(
          "/as/self-assessment/account/1234567890/liability-details"
        )).willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.stringify(successfulHipResponse))
        )
      )

      val request = FakeRequest(
        GET,
        "/1234567890?fromDate=2024-01-01"
      )

      val result = route(app, request).get

      status(result) mustBe OK

      verifyRequestReceivedAudit(OK)
    }

    "audit statusCode and responseMessage for an invalid UTR" in {

      stubAuditEndpoint()

      val request =
        FakeRequest(
          GET,
          "/invalidUtr?fromDate=2024-01-01"
        )

      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST

      verifyRequestReceivedAudit(BAD_REQUEST)
    }

    "audit statusCode and responseMessage when a downstream call fails" in {

      stubAuditEndpoint()

      server.stubFor(
        get(
          urlEqualTo(
            "/citizen-details/sautr/1234567890"
          )
        ).willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withHeader(
              "Content-Type",
              "application/json"
            )
            .withBody(
              """{"message":"downstream failed"}"""
            )
        )
      )

      val request =
        FakeRequest(
          GET,
          "/1234567890?fromDate=2024-01-01"
        )

      val result = route(app, request).get

      status(result) mustBe INTERNAL_SERVER_ERROR

      verifyRequestReceivedAudit(INTERNAL_SERVER_ERROR)
    }

    "audit statusCode and responseMessage for an authentication failure" in {

      stubAuditEndpoint()

      when(
        mockAuthConnector.authorise[RetrievalsType](
          any[Predicate](),
          any[Retrieval[RetrievalsType]]()
        )(
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(
        Future.failed(InsufficientEnrolments())
      )

      val request =
        FakeRequest(
          GET,
          "/1234567890?fromDate=2024-01-01"
        )

      val result = route(app, request).get

      status(result) mustBe UNAUTHORIZED

      verifyRequestReceivedAudit(UNAUTHORIZED)
    }

    "audit statusCode and responseMessage for an unsupported HTTP method" in {

      stubAuditEndpoint()

      val request =
        FakeRequest(
          POST,
          "/1234567890?fromDate=2024-01-01"
        )

      val result = route(app, request).get

      status(result) mustBe NOT_FOUND

      verifyRequestReceivedAudit(NOT_FOUND)
    }

    "audit statusCode and responseMessage for an invalid fromDate" in {

      stubAuditEndpoint()

      val request =
        FakeRequest(
          GET,
          "/1234567890?fromDate=not-a-date"
        )

      val result = route(app, request).get

      status(result) mustBe BAD_REQUEST

      verifyRequestReceivedAudit(BAD_REQUEST)
    }

    "audit statusCode and responseMessage for insufficient confidence" in {

      stubAuditEndpoint()

      when(
        mockAuthConnector.authorise[RetrievalsType](
          any[Predicate](),
          any[Retrieval[RetrievalsType]]()
        )(
          any[HeaderCarrier](),
          any[ExecutionContext]()
        )
      ).thenReturn(
        Future.successful(
          new~(Some(Individual), ConfidenceLevel.L50)
        )
      )

      val request =
        FakeRequest(
          GET,
          "/1234567890?fromDate=2024-01-01"
        )

      val result = route(app, request).get

      status(result) mustBe UNAUTHORIZED

      verifyRequestReceivedAudit(UNAUTHORIZED)
    }
  }
}
