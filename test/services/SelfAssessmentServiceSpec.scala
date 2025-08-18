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
import models.MtdId
import models.ServiceErrors.Downstream_Error
import org.mockito.ArgumentMatchers.{any, eq as meq}
import org.mockito.Mockito.when
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import shared.SpecBase

import scala.concurrent.Future

class SelfAssessmentServiceSpec
    extends SpecBase{



  val mockCitizenDetailsConnector: CitizenDetailsConnector = mock[CitizenDetailsConnector]
  val mockMtdConnector: MtdIdentifierLookupConnector = mock[MtdIdentifierLookupConnector]
  val mockHipConnector: HipConnector = mock[HipConnector]

  val service: SelfAssessmentService = new SelfAssessmentService(mockCitizenDetailsConnector, mockMtdConnector, mockHipConnector)

  val testUtr = "1234567890"
  val testNino = "AB123456C"
  val testMtdId = "XAIT0000123456"

  "getMtdIdFromUtr" should {
    "return mtdId when both connectors return successful responses" in {
      when(mockCitizenDetailsConnector.getNino(meq(testUtr))(any(), any()))
        .thenReturn(Future.successful(Some(testNino)))

      when(mockMtdConnector.getMtdId(meq(testNino))(any(), any()))
        .thenReturn(Future.successful(MtdId(testMtdId)))

      val result = service.getMtdIdFromUtr(testUtr)

      result.futureValue mustBe testMtdId
    }

    "fail with Downstream_Error when CitizenDetailsConnector returns no NINO" in {
      when(mockCitizenDetailsConnector.getNino(meq(testUtr))(any(), any()))
        .thenReturn(Future.successful(None))

      val result = service.getMtdIdFromUtr(testUtr)

      result.failed.futureValue mustBe Downstream_Error
    }

    "fail with Downstream_Error when CitizenDetailsConnector fails" in {
      when(mockCitizenDetailsConnector.getNino(meq(testUtr))(any(), any()))
        .thenReturn(Future.failed(Downstream_Error))

      val result = service.getMtdIdFromUtr(testUtr)

      result.failed.futureValue mustBe Downstream_Error
    }

    "fail with Downstream_Error when MtdIdentifierLookupConnector fails" in {
      when(mockCitizenDetailsConnector.getNino(meq(testUtr))(any(), any()))
        .thenReturn(Future.successful(Some(testNino)))

      when(mockMtdConnector.getMtdId(meq(testNino))(any(), any()))
        .thenReturn(Future.failed(Downstream_Error))

      val result = service.getMtdIdFromUtr(testUtr)

      result.failed.futureValue mustBe Downstream_Error
    }

    "correctly pass the NINO from the first connector to the second connector" in {
      val anotherNino = "XY987654Z"

      when(mockCitizenDetailsConnector.getNino(meq(testUtr))(any(), any()))
        .thenReturn(Future.successful(Some(anotherNino)))

      when(mockMtdConnector.getMtdId(meq(anotherNino))(any(), any()))
        .thenReturn(Future.successful(MtdId(testMtdId)))

      val result = service.getMtdIdFromUtr(testUtr)

      result.futureValue mustBe testMtdId
    }
  }
  "viewAccountService" should {

    "call HIP with " in {
      
    }
  }
}
