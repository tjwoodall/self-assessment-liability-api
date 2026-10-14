/*
 * Copyright 2026 HM Revenue & Customs
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

package filters

import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.streams.Accumulator
import play.api.mvc.{EssentialAction, Result}
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.{ExecutionContext, Future}

class ExceptionRecoveryFilterSpec extends AnyWordSpec with Matchers {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "ExceptionRecoveryFilter" should {

    "convert an unexpected exception into a 503 response" in {
      val application = new GuiceApplicationBuilder().build()

      running(application) {
        implicit val mat: Materializer =
          application.injector.instanceOf[Materializer]

        val filter = new ExceptionRecoveryFilter()

        val failingAction: EssentialAction = EssentialAction { _ =>
          Accumulator.done[Result](
            Future.failed(new RuntimeException("Test failure"))
          )
        }

        val filteredAction = filter(failingAction)
        val result = filteredAction(FakeRequest()).run()

        status(result).shouldBe(SERVICE_UNAVAILABLE)
      }
    }

    "preserve a successful response" in {
      val application = new GuiceApplicationBuilder().build()

      running(application) {
        implicit val mat: Materializer =
          application.injector.instanceOf[Materializer]

        val filter = new ExceptionRecoveryFilter()

        val successfulAction: EssentialAction = EssentialAction { _ =>
          Accumulator.done[Result](Ok("Success"))
        }

        val filteredAction = filter(successfulAction)
        val result = filteredAction(FakeRequest()).run()

        status(result).shouldBe(OK)
      }
    }
  }
}
