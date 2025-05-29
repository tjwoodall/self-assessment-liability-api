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

import play.api.http.Status.OK

class CitizenDetailsConnectorSpec extends BaseConnectorSpec {
  "CitizenDetailsConnector" should {
    "return Some(nino) when the backend returns status 200 and all expected" in {

    }
    "return None when the backend returns status 200 without nino" in {

    }
    "return Error when the backend returns status 400" in {

    }
    "return Error when the backend returns status 404" in {

    }
    "return Error when the backend returns status 500" in {

    }
    "return Error when the backend returns any other status" in {

    }
  }
}
