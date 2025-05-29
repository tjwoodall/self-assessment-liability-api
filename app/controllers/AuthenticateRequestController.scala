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
import play.api.mvc.{ActionBuilder, AnyContent, BodyParser, ControllerComponents, Request, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticateRequestController (cc: ControllerComponents, override val authConnector: AuthConnector)(implicit appConfig: AppConfig, ec: ExecutionContext)
        extends BackendController(cc) with AuthorisedFunctions{
  
  def authorisedAction(utr: String):ActionBuilder[RequestData, AnyContent] = new ActionBuilder[RequestData, AnyContent]{
    override def parser: BodyParser[AnyContent] = cc.parsers.defaultBodyParser

    override protected def executionContext: ExecutionContext = cc.executionContext


    override def invokeBlock[A](request: Request[A], block: RequestData[A] => Future[Result]): Future[Result] = {
      implicit val headerCarrier: HeaderCarrier = hc(request)

      authorised(selfAssessmentEnrolments(utr)) {
       block(RequestData(utr,request))
      }
    }
    
    
    
    
    private def selfAssessmentEnrolments(utr:String): Predicate = {
      (Individual and Enrolment("IR-SA").withIdentifier("UTR", utr)) or
        (Individual and checkForMtdEnrolment(utr) )
    }

    private def checkForMtdEnrolment(utr:String): Predicate ={
      ???
    }
  }
}
