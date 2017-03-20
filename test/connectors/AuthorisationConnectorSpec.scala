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

package connectors

import config.AppConfig
import models.{AuthorityModel, Enrolment, Identifier}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.http.Status._
import play.api.inject.Injector
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class AuthorisationConnectorSpec extends UnitSpec with MockitoSugar with OneAppPerSuite {

  implicit val hc = HeaderCarrier()

  lazy val injector: Injector = app.injector
  lazy val mockConfig: AppConfig = injector.instanceOf[AppConfig]

  lazy val mockHttp: HttpGet = mock[HttpGet]

  lazy val target = new AuthorisationConnector(mockConfig) {
    override val http: HttpGet = mockHttp
  }

  lazy val authorityResponse: JsValue = Json.parse(
    s"""{"uri":"/auth/oid/57e915480f00000f006d915b","confidenceLevel":200,"credentialStrength":"strong",
       |"userDetailsLink":"http://localhost:9978/user-details/id/000000000000000000000000","legacyOid":"00000000000000000000000",
       |"new-session":"/auth/oid/57e915480f00000f006d915b/session","ids":"/auth/oid/57e915480f00000f006d915b/ids",
       |"credentials":{"gatewayId":"000000000000000"},"accounts":{"paye":{"link":"test","nino":"NININININININO"}},"lastUpdated":"2016-09-26T12:32:08.734Z",
       |"loggedInAt":"2016-09-26T12:32:08.734Z","levelOfAssurance":"1",
       |"enrolments":[{"key":"HMRC_AGENT_AGENT_KEY","identifiers":[{"key":"Identifier","value":"Value"}],"state":"State"}],
       |"affinityGroup":"Agent",
       |"correlationId":"0000000000000000000000000000000000000000000000000000000000000000","credId":"000000000000000"}""".stripMargin
  )

  "Calling the Authorisation Connectors .getAuthority" when {

    "called with a valid request" should {

      when(mockHttp.GET[HttpResponse](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK, Some(authorityResponse))))

      lazy val result = await(target.getAuthority())

      "return an AuthorityModel" in {
        result shouldBe a[AuthorityModel]
      }

      "return an AffinityGroup of Agent" in {
        result.affinityGroup shouldBe "Agent"
      }

      "return an set of enrolments" in {
        result.enrolments shouldBe a[Set[Enrolment]]
      }

      "return a set of enrolments that contains the key HMRC_AGENT_AGENT_KEY" in {
        result.enrolments.contains(Enrolment("HMRC_AGENT_AGENT_KEY", Seq(Identifier("Identifier", "Value")), "State")) shouldBe true
      }
    }

    "the Connection responds with a not an OK status" in {
      when(mockHttp.GET[HttpResponse](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, Some(authorityResponse))))

      lazy val result = await(target.getAuthority())

      lazy val exception = intercept[Exception] {
        await(result)
      }

      exception.getMessage shouldBe s"The request for the authority returned a $BAD_REQUEST"
    }
  }
}
