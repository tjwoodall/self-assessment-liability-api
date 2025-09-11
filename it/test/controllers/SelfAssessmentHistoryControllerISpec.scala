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

import models.{CidPerson, MtdId, TaxIds}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, contentAsJson, defaultAwaitTimeout, route, status}
import shared.HipResponseGenerator
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.http.HttpClientV2Provider
import utils.IntegrationSpecBase

import java.time.LocalDate
import scala.concurrent.Await

class SelfAssessmentHistoryControllerISpec extends IntegrationSpecBase {


  lazy val provider: HttpClientV2Provider = app.injector.instanceOf[HttpClientV2Provider]
  lazy val client: HttpClientV2 = provider.get()
  val cidPerson: CidPerson = CidPerson(
    name = None,
    ids = TaxIds(nino = Some(nino)),
    dateOfBirth = None
  )
  val dateFrom: String = LocalDate.now().minusYears(2).toString
  val dateTo: String = LocalDate.now().toString
  val cidPayload: String = Json.toJson(cidPerson).toString
  val mtdId = MtdId("mtdId")
  val mtdIdPayload: String = Json.obj("mtdbsa" -> "mtdId").toString
  val baseUrl = s"http://localhost:$port/$utr?fromDate=$dateFrom"
  val hipUrl = s"/self-assessment/account/$utr/liability-details?dateFrom=$dateFrom&dateTo=$dateTo"
  val cidUrl = s"/citizen-details/sautr/$utr"
  val mtdLookupUrl = s"/mtd-identifier-lookup/nino/$nino"


  "Integration Tests for SA History Controller" should {
    "should return 200 with the correct response in success journey" in {

      forAll(HipResponseGenerator.hipResponseGen) { hipResponse =>
        val hipResponsePayload = Json.toJson(hipResponse)
        simulateGet(cidUrl, OK, cidPayload)
        simulateGet(mtdLookupUrl, OK, mtdIdPayload)
        simulateGet(hipUrl, OK, hipResponsePayload.toString)
        val request = FakeRequest(GET, s"/$utr?fromDate=$dateFrom")
          .withHeaders("Authorization" -> "Bearer 1234")

        val result = route(app, request).get

        status(result) mustEqual OK
        contentAsJson(result) mustEqual hipResponsePayload
      }
    }
    "should return 500 if call to CID fails due to data quality issues" in {
        simulateGet(cidUrl, OK, "")
        val request = FakeRequest(GET, s"/$utr?fromDate=$dateFrom")
          .withHeaders("Authorization" -> "Bearer 1234")
        val result = route(app, request).get
        status(result) mustEqual INTERNAL_SERVER_ERROR
    }
    "should return 500 if call to CID fails due to data quality issues s" in {
      simulateGet(cidUrl, NOT_FOUND, "")
      val request = FakeRequest(GET, s"/$utr?fromDate=$dateFrom")
        .withHeaders("Authorization" -> "Bearer 1234")
      val result = route(app, request).get
      status(result) mustEqual INTERNAL_SERVER_ERROR
    }
  }
}
