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
import models.{HipResponse, TaxPeriod}
import models.ServiceErrors.{Downstream_Error, Invalid_Start_Date}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelfAssessmentService @Inject() (
    cidConncetor: CitizenDetailsConnector,
    mtdConnector: MtdIdentifierLookupConnector,
    hipConnector: HipConnector
)(implicit ec: ExecutionContext) {
  def getMtdIdFromUtr(utr: String)(implicit hc: HeaderCarrier): Future[String] = {
    for {
      maybeNino <- cidConncetor.getNino(utr)
      mtdId <- maybeNino.map(mtdConnector.getMtdId(_)).getOrElse(Future.failed(Downstream_Error))
    } yield mtdId.mtdbsa
  }

  def viewAccountService(utr: String, fromDate: Option[String])(implicit hc: HeaderCarrier): Future[HipResponse] = {
    for {
      taxYears <- getTaxYears(fromDate)
      hipResponse <- hipConnector.getSelfAssessmentData(utr, taxYears.dateFrom.toString, taxYears.dateTo.toString)
    } yield hipResponse
  }
  /*
  6 +1 and not after today
  
   */
  private def getTaxYears(fromDate: Option[String]): Future[TaxPeriod] = {
    val today = LocalDate.now()
      val startingDate = fromDate.map { date => taxYearStart(LocalDate.parse(date)) }.getOrElse(taxYearStart(today))
      Future.successful(TaxPeriod(dateFrom = startingDate, dateTo = today))
  }
  
  /*
  1) 03-4-2015 -> 
   */
  
  private def taxYearStart(date: LocalDate): LocalDate ={
    date.withMonth(4).withDayOfMonth(6)
  }
}
