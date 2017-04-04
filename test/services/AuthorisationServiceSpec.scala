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

package services

import java.util.UUID

import connectors.AuthorisationConnector
import models.{AuthAuthorityModel, AuthorityModel, Enrolment, Identifier}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class AuthorisationServiceSpec extends UnitSpec with OneAppPerSuite with MockitoSugar {

  implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  def setupService(authAuthority: Future[AuthAuthorityModel], enrolments: Future[Set[Enrolment]]): AuthorisationService = {

    val mockConnector = mock[AuthorisationConnector]

    when(mockConnector.getAuthority()(ArgumentMatchers.any()))
      .thenReturn(authAuthority)

    when(mockConnector.getEnrolmentsResponse(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(enrolments)

    new AuthorisationService(mockConnector)
  }

  val authAuthorityModel = AuthAuthorityModel("ARN", "EnrolmentUrl")
  val enrolments = Set(Enrolment("Key", Seq(Identifier("key", "value")), "Status"))
  val authModel = AuthorityModel("ARN", Set(Enrolment("Key", Seq(Identifier("key", "value")), "Status")))

  "Calling the .getUserAuthority" should {

    "return a valid Authority model" in {
      lazy val mockedService = setupService(authAuthorityModel, enrolments)

      await(mockedService.getUserAuthority()) shouldBe authModel
    }

    "return an error when the authority connector throws one" in {
      lazy val mockedService = setupService(throw new Exception("Error message"), enrolments)
      lazy val ex = intercept[Exception] {
        await(mockedService.getUserAuthority())
      }

      ex.getMessage shouldBe "Error message"
    }

    "return an error when the enrolments connector throws one" in {
      lazy val mockedService = setupService(authAuthorityModel, throw new Exception("Error message"))
      lazy val ex = intercept[Exception] {
        await(mockedService.getUserAuthority())
      }

      ex.getMessage shouldBe "Error message"
    }
  }
}
