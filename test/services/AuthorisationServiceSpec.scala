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
import models.{AuthorityModel, Enrolment, Identifier}
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

  def setupService(authority: Future[AuthorityModel]): AuthorisationService = {

    val mockConnector = mock[AuthorisationConnector]

    when(mockConnector.getAuthority()(ArgumentMatchers.any()))
      .thenReturn(Future.successful(authority))

    new AuthorisationService(mockConnector)
  }

  val authorityModel = AuthorityModel("Agent", Set(Enrolment("Key", Seq(Identifier("key", "value")), "Status")))

  "Calling the .getUserAuthority" should {

    "return a valid Authority model" in {
      lazy val mockedService = setupService(authorityModel)

      await(mockedService.getUserAuthority()) shouldBe authorityModel
    }

    "return an error when the connector throws one" in {
      lazy val mockedService = setupService(throw new Exception("Error message"))
      lazy val ex = intercept[Exception] {
        await(mockedService.getUserAuthority())
      }

      ex.getMessage shouldBe "Error message"
    }
  }
}
