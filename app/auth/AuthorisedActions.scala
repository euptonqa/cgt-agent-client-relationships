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

package auth

import javax.inject.{Inject, Singleton}

import checks.{AffinityGroupCheck, EnrolmentCheck}
import play.api.mvc.Result
import services.AuthorisationService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class AuthorisedActions @Inject()(authorisationService: AuthorisationService) {

  def authorisedAgentAction(action: Boolean => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    for {
      authority <- authorisationService.getUserAuthority()
      affinityGroupCheck <- AffinityGroupCheck.affinityGroupCheckAgent(authority.affinityGroup)
      enrolmentCheck <- EnrolmentCheck.checkEnrolments(Some(authority.enrolments.toSeq))
      result <- action(affinityGroupCheck && enrolmentCheck)
    } yield result
  }
}
