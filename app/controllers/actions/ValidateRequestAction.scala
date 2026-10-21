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

import cats.data.EitherT
import com.google.inject.Inject
import models.{ApiErrorResponses, RequestPeriod, RequestWithUtr}
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.BadRequest
import utils.UkTaxYears.{getPastTwoUkTaxYears, isInvalidDate}
import utils.UtrValidator.isValidUtr
import utils.constants.ErrorMessageConstansts.BAD_REQUEST_RESPONSE

import java.time.LocalDate
import java.time.format.DateTimeParseException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ValidateRequestAction @Inject() ()(implicit val ec: ExecutionContext) extends Logging {

  def apply(utr: String): ActionRefiner[Request, RequestWithUtr] =
    new ActionRefiner[Request, RequestWithUtr] {

      override protected def executionContext: ExecutionContext = ec

      override protected def refine[A](
          request: Request[A]
      ): Future[Either[Result, RequestWithUtr[A]]] = {

        val validation = for {
          _ <- EitherT.cond[Future](
            isValidUtr(utr),
            (),
            badRequest
          )
          requestPeriod = getPastTwoUkTaxYears()
          startDate <- request.getQueryString("fromDate") match {
            case Some(date) =>
              validateAndParseDate(date)
            case None =>
              EitherT.rightT[Future, Result](requestPeriod._1)
          }
        } yield RequestWithUtr(
          utr = utr,
          requestPeriod = RequestPeriod(
            startDate = startDate,
            endDate = requestPeriod._2
          ),
          request = request
        )

        validation.value
      }

      private def validateAndParseDate(
          dateInStringFormat: String
      ): EitherT[Future, Result, LocalDate] = {

        EitherT {
          Future.successful {
            Try(LocalDate.parse(dateInStringFormat)).toEither match {
              case Right(parsedDate) if !isInvalidDate(dateToValidate = parsedDate) =>
                Right(parsedDate)

              case Right(_) =>
                logger.info(s"Rejecting $dateInStringFormat as it is invalid")
                Left(badRequest)

              case Left(_: DateTimeParseException) =>
                logger.info(s"parsing of $dateInStringFormat failed")
                Left(badRequest)

              case Left(error) =>
                throw error
            }
          }
        }
      }

      private def badRequest: Result =
        BadRequest(
          ApiErrorResponses(BAD_REQUEST_RESPONSE).asJson
        )
    }
}
