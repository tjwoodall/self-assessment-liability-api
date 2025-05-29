package connectors

import config.AppConfig

import play.api.Logging
import play.api.libs.json.JsValue
import scala.concurrent.{ExecutionContext, Future}
import java.net.URL
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

@Singleton
class CitizenDetailsConnector @Inject() (appConfig: AppConfig, http: HttpClientV2)(implicit ec: ExecutionContext) extends Logging {
  def searchByUtr(utr: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    http
      .get(new URL(s"appConfig.citizenDetailsBaseUrl/citizen-details/sautr/$utr"))
//      .get(new URL(s"${appConfig.citizenDetailsBaseUrl}/citizen-details/sautr/$utr"))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case 200 => extractNinoFromResponse(response.json)
          case 400 => None
          case 404 => None
          case 500 => None
          case _   => None
        }
      }
      .recoverWith { case t: Throwable =>
        logger.warn("Unexpected response from Citizen Details", t)
        Future.failed(new RuntimeException("Citizen Details", t))
      }

  private def extractNinoFromResponse(json: JsValue): Option[String] = {
    val currentFirstName: Option[String] = (json \ "name" \ "current" \ "firstName").asOpt[String]
    val currentLastName: Option[String] = (json \ "name" \ "current" \ "lastName").asOpt[String]
    val nino: Option[String] = (json \ "ids" \ "nino").asOpt[String]
    val dateOfBirth: Option[String] = (json \ "dateOfBirth").asOpt[String]

    if (currentFirstName.isEmpty || currentLastName.isEmpty || dateOfBirth.isEmpty) {
      // TODO: Response body format change.
    }
    if (nino.isEmpty) {
      // TODO: No NINO for UTR.
    }

    nino
  }
}
