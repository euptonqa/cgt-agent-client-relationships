/*
 * Copyright 2017 HM Revenue & Customs
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

package checks

import common.Constants.AffinityGroup._
import uk.gov.hmrc.play.test.UnitSpec

class AffinityGroupCheckSpec extends UnitSpec{

  "Calling .affinityGroupCheckAgent" should {
    "return true when supplied with an agent user" in {
      await(AffinityGroupCheck.affinityGroupCheckAgent(Agent)) shouldBe true
    }

    "return false when supplied with an organisation user" in {
      await(AffinityGroupCheck.affinityGroupCheckAgent(Organisation)) shouldBe false
    }

    "return false when supplied with an individual user" in {
      await(AffinityGroupCheck.affinityGroupCheckAgent(Individual)) shouldBe false
    }
  }
}
