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
import connectors.HipConnector
import models.ApiErrorResponses
import models.ServiceErrors.*
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.SelfAssessmentService
import uk.gov.hmrc.auth.core.AuthConnector

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelfAssessmentHistoryController @Inject() (
    override val authConnector: AuthConnector,
    val service: SelfAssessmentService,
    val hipConnector: HipConnector,
    cc: ControllerComponents
)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends AuthenticateRequestController(cc, service, authConnector) {

  private val badRequest = Future.successful(
    BadRequest(
      ApiErrorResponses(
        "Bad Request",
        "Invalid request format or parameters"
      ).asJson
    )
  )
  private val unauthorised = Future.successful(
    Unauthorized(
      ApiErrorResponses(
        "Unauthorised",
        "Invalid request format or parameters"
      ).asJson
    )
  )
  private val forbidden = Future.successful(
    Forbidden(
      ApiErrorResponses(
        "Forbidden",
        "Access not permitted"
      ).asJson
    )
  )
  private val internalError = Future.successful(
    InternalServerError(
      ApiErrorResponses(
        "Internal Server Error",
        "Unexpected internal error. Please try again later."
      ).asJson
    )
  )
  private val serviceUnavailable = Future.successful(
    ServiceUnavailable(
      ApiErrorResponses(
        "Service Unavailable",
        "Service unavailable. Pleased try again later"
      ).asJson
    )
  )

  def getYourSelfAssessmentData(utr: String, fromDate: String): Action[AnyContent] =
    authorisedAction(utr) { implicit request =>
      hipConnector
        .getSelfAssessmentData(utr, fromDate)
        .flatMap { jsValue =>
          Future.successful(Ok(jsValue))
        }
        .recoverWith {
          case _: Invalid_Correlation_Id.type    => internalError
          case _: HIP_Unauthorised.type          => unauthorised
          case _: HIP_Forbidden.type             => forbidden
          case _: No_Payments_Found_For_UTR.type => badRequest
          case _: Invalid_UTR.type               => internalError
          case _: HIP_Server_Error.type          => internalError
          case _: HIP_Bad_Gateway.type           => internalError
          case _: HIP_Service_Unavailable.type   => serviceUnavailable
          case _: Downstream_Error.type          => internalError
        }
    }
}
