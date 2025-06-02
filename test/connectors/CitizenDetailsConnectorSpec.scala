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

package connectors

import models.ApiErrorResponses
import shared.{HttpWireMock, SpecBase}
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, IM_A_TEAPOT, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.inject.guice.GuiceApplicationBuilder

class CitizenDetailsConnectorSpec extends SpecBase with HttpWireMock {
  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      conf = "microservice.services.citizen-details.port" -> server.port()
    )
    .build()
  private lazy val connector: CitizenDetailsConnector =
    app.injector.instanceOf[CitizenDetailsConnector]
  private def serviceUrl(utr: String) = s"/citizen-details/sautr/$utr"
  private val nino: String = "AA055075C"
  private val validSuccessResponse: String = """
  {
    "name": {
      "current": {
        "firstName": "John",
        "lastName": "Smith"
      },
      "previous": []
    },
    "ids": {
      "nino": "AA055075C"
    },
    "dateOfBirth": "11121971"
  }
  """
  private val badRequestResponse: ApiErrorResponses = ApiErrorResponses.apply(
    status = 400,
    message = "Invalid SaUtr."
  )
  private val notFoundResponse: ApiErrorResponses = ApiErrorResponses.apply(
    status = 404,
    message = "No record for the given SaUtr is found."
  )
  private val internalServerErrorResponse: ApiErrorResponses = ApiErrorResponses.apply(
    status = 500,
    message = "More than one valid matching result."
  )

  "CitizenDetailsConnector" should {
    "return nino when the backend returns status 200 and all expected" in {
      simmulateGet(serviceUrl("utr"), OK, validSuccessResponse)
      val result = connector.getNino("utr")
      result.futureValue mustBe nino
    }
    "return Error when the backend returns status 400" in {
      simmulateGet(serviceUrl("invalidUtr"), BAD_REQUEST, "")
      val result = connector.getNino("invalidUtr")
      result.failed.futureValue mustBe badRequestResponse
    }
    "return Error when the backend returns status 404" in {
      simmulateGet(serviceUrl("invalidUtr"), NOT_FOUND, "")
      val result = connector.getNino("invalidUtr")
      result.failed.futureValue mustBe notFoundResponse
    }
    "return Error when the backend returns status 500" in {
      simmulateGet(serviceUrl("invalidUtr"), INTERNAL_SERVER_ERROR, "")
      val result = connector.getNino("invalidUtr")
      result.failed.futureValue mustBe internalServerErrorResponse
    }
    "return Error when the backend returns any other status" in {
      simmulateGet(serviceUrl("invalidUtr"), IM_A_TEAPOT, "")
      val result = connector.getNino("invalidUtr")
      result.failed.futureValue mustBe ApiErrorResponses.apply(
        status = 500,
        message = "Service currently unavailable"
      )
    }
  }
}
