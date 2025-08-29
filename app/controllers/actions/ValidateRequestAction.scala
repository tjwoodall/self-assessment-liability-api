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

package controllers.actions

import com.google.inject.Inject
import models.ServiceErrors.{Invalid_Start_Date_Error, Invalid_Utr_Error}
import models.{RequestPeriod, RequestWithUtr}
import play.api.mvc.{ActionTransformer, *}
import utils.UtrValidator.isValidUtr
import utils.constants.UkTaxYears.{GetPastTwoUkTaxYears, isInvalidDate}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

case class ValidateRequestAction @Inject() (
    parser: BodyParsers.Default
)(implicit val ec: ExecutionContext) {

  def apply(utr: String): ActionTransformer[Request, RequestWithUtr] =
    new ActionTransformer[Request, RequestWithUtr] {

      override protected def executionContext: ExecutionContext = ec
      override protected def transform[A](request: Request[A]): Future[RequestWithUtr[A]] = {

        if (isValidUtr(utr)) {
          val requestPeriod = GetPastTwoUkTaxYears()
          request
            .getQueryString("fromDate")
            .fold(
              Future.successful(
                RequestWithUtr(
                  utr = utr,
                  requestPeriod =
                    RequestPeriod(startDate = requestPeriod._1, endDate = requestPeriod._2),
                  request = request
                )
              )
            ) { dateStr =>
              validateAndParseDate(dateStr).map(date =>
                RequestWithUtr(
                  utr = utr,
                  requestPeriod = RequestPeriod(startDate = date, endDate = requestPeriod._2),
                  request = request
                )
              )
            }
        } else {
          Future.failed(Invalid_Utr_Error)
        }
      }

      private def validateAndParseDate(dateStr: String): Future[LocalDate] = {
        val date = LocalDate.parse(dateStr)
        if (isInvalidDate(dateToValidate = date)) Future.failed(Invalid_Start_Date_Error)
        else Future.successful(date)
      }

    }

}
