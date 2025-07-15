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

package models

import play.api.mvc.Result
import play.api.mvc.Results.*
import scala.concurrent.Future

import utils.FutureConverter.FutureOps

class StandardErrorResponses {}

object StandardErrorResponses {
  final val badRequest: Future[Result] = BadRequest(
    ApiErrorResponses(
      "Bad Request",
      "Invalid request format or parameters"
    ).asJson
  ).toFuture

  final val unauthorised: Future[Result] = Unauthorized(
    ApiErrorResponses(
      "Unauthorised",
      "Authorisation failed"
    ).asJson
  ).toFuture

  final val forbidden: Future[Result] = Forbidden(
    ApiErrorResponses(
      "Forbidden",
      "Access not permitted"
    ).asJson
  ).toFuture

  final val internalServerError: Future[Result] = InternalServerError(
    ApiErrorResponses(
      "Internal Server Error",
      "Unexpected internal error. Please try again later."
    ).asJson
  ).toFuture

  final val serviceUnavailable: Future[Result] = ServiceUnavailable(
    ApiErrorResponses(
      "Service Unavailable",
      "Service unavailable. Pleased try again later."
    ).asJson
  ).toFuture
}
