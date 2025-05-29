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

import config.AppConfig
import models.Error

import play.api.Logging
import play.api.libs.json.JsValue
import scala.concurrent.{ExecutionContext, Future}
import java.net.URL
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

@Singleton
class CitizenDetailsConnector @Inject() (appConfig: AppConfig, http: HttpClientV2)(implicit ec: ExecutionContext) extends Logging {
  def searchByUtr(utr: String)(implicit hc: HeaderCarrier): Future[Either[Error, Option[String]]] =
    http
      .get(new URL(s"${appConfig.citizenDetailsBaseUrl}/citizen-details/sautr/$utr"))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case 200 => extractNinoFromResponse(response.json)
          case 400 => Error(response.status, "Invalid SaUtr.")
          case 404 => Error(response.status, "No record for the given SaUtr is found.")
          case 500 => Error(response.status, "More than one valid matching result.")
          case _   => Error(response.status, "Unexpected result.")
        }
      }
      .recoverWith { case t: Throwable =>
        logger.warn("Unexpected response from Citizen Details", t)
        Future.failed(new RuntimeException("Citizen Details", t))
      }

  private def extractNinoFromResponse(json: JsValue): Option[String] = {
    (json \ "ids" \ "nino").asOpt[String]
  }
}
