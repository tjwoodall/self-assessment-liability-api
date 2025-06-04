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
import models.ServiceErrors.Downstream_Error
import models.{ApiErrorResponses, RequestData}
import play.api.mvc.*
import services.SelfAssessmentService
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, authorisedEnrolments}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticateRequestController(
    cc: ControllerComponents,
    selfAssessmentService : SelfAssessmentService,
    override val authConnector: AuthConnector
)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends BackendController(cc)
    with AuthorisedFunctions {

  def authorisedAction(utr: String)(block: RequestData[AnyContent] => Future[Result]): Action[AnyContent] = {
    Action.async(cc.parsers.anyContent) { request =>
      implicit val headerCarrier: HeaderCarrier = hc(request)
    
        authorised(selfAssessmentEnrolments(utr)) {
          Future.successful(Right(RequestData(utr, None, request)))
        }
          .recoverWith { case _: AuthorisationException =>
            selfAssessmentService.getMtdIdFromUtr(utr).flatMap { id =>
              authorised(checkForMtdEnrolment(id)).retrieve(affinityGroup) {
                case Some(Individual) =>
                  Future.successful(Right(RequestData(utr, None, request)))
                case Some(Organisation) =>
                  Future.successful(Right(RequestData(utr, None, request)))
                case Some(Agent) =>
                  authorised(agentDelegatedEnrolments(utr, id)) {
                    Future.successful(Right(RequestData(utr, None, request)))
                  }
                case _ => Future.successful(Left(InternalServerError(ApiErrorResponses(Downstream_Error.toString, "unsupported affinity group").asJson)))
              }
            }.recoverWith {
              case _: ApiErrorResponses => Future.successful(Left(InternalServerError(ApiErrorResponses(Downstream_Error.toString, "unsupported affinity group").asJson)))
            }
          }.flatMap {
          case Right(requestData) => block(requestData)
          case Left(result) => Future.successful(result)
        }
    }
  }

      private def selfAssessmentEnrolments(utr: String): Predicate = {
        (Individual and Enrolment("IR-SA").withIdentifier("UTR", utr)) or
          (Organisation and Enrolment("IR-SA").withIdentifier("UTR", utr))
      }

      private def checkForMtdEnrolment(mtdId: String): Predicate = {
        (Individual and Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", mtdId)) or
          (Organisation and Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", mtdId)) or
          (Agent and Enrolment("HMRC-AS-AGENT"))
      }

      private def agentDelegatedEnrolments(utr: String, mtdId: String): Predicate = {
        Enrolment("HMRC-MTD-IT")
          .withIdentifier("MTDITID", mtdId)
          .withDelegatedAuthRule("mtd-it-auth") or
          Enrolment("IR-SA").withIdentifier("UTR", utr).withDelegatedAuthRule("sa-auth")
      }
}