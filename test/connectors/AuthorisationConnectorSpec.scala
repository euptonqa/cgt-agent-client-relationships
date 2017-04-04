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
import models.{AuthAuthorityModel, AuthorityModel, Enrolment, Identifier}
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


  def createTarget(mockResponse: HttpResponse): AuthorisationConnector = {
    lazy val mockHttp: HttpGet = mock[HttpGet]

    when(mockHttp.GET[HttpResponse](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(mockResponse))

    new AuthorisationConnector(mockConfig) {
      override val http: HttpGet = mockHttp
    }
  }

  lazy val authorityResponse: JsValue = Json.parse(
    s"""{"uri":"/auth/oid/57e915480f00000f006d915b","confidenceLevel":200,"credentialStrength":"strong",
       |"userDetailsLink":"http://localhost:9978/user-details/id/000000000000000000000000","legacyOid":"00000000000000000000000",
       |"new-session":"/auth/oid/57e915480f00000f006d915b/session","ids":"/auth/oid/57e915480f00000f006d915b/ids",
       |"credentials":{"gatewayId":"000000000000000"},"accounts":{"paye":{"link":"test","nino":"NININININININO"}},"lastUpdated":"2016-09-26T12:32:08.734Z",
       |"loggedInAt":"2016-09-26T12:32:08.734Z","levelOfAssurance":"1",
       |"enrolments":"enrolment-uri",
       |"affinityGroup":"Agent",
       |"correlationId":"0000000000000000000000000000000000000000000000000000000000000000","credId":"000000000000000"}""".stripMargin
  )

  "Calling the Authorisation Connectors .getAuthority" when {

    "called with a valid request" should {

      lazy val target = createTarget(HttpResponse(OK, Some(authorityResponse)))
      lazy val result = await(target.getAuthority())

      "return an AuthorityModel" in {
        result shouldBe a[AuthAuthorityModel]
      }

      "return an AffinityGroup of Agent" in {
        result.affinityGroup shouldBe "Agent"
      }

      "return an set of enrolments" in {
        result.enrolmentsUrl shouldBe "enrolment-uri"
      }
    }

    "the Connection responds with a not an OK status" in {
      lazy val target = createTarget(HttpResponse(BAD_REQUEST, Some(authorityResponse)))
      lazy val result = await(target.getAuthority())

      lazy val exception = intercept[Exception] {
        await(result)
      }

      exception.getMessage shouldBe s"The request for the authority returned a $BAD_REQUEST"
    }
  }

  lazy val enrolmentJson: JsValue = Json.parse(
    s"""[{"key":"EnrolmentKey","identifiers":[{"key":"IDKey","value":"IdentifierValue"}],"state":"State"}]""".stripMargin
  )

  "Calling AuthorisationConnector.getEnrolmentsResponse" when {

    "the connection returns an OK" should {

      lazy val target = createTarget(HttpResponse(OK, Some(enrolmentJson)))
      lazy val result = await(target.getEnrolmentsResponse("test-url"))

      "return a set of enrolments" in {
        result shouldBe a[Set[Enrolment]]
      }

      "have the key EnrolmentKey" in {
        result.contains(Enrolment("EnrolmentKey", Seq(Identifier("IDKey", "IdentifierValue")), "State"))
      }
    }

    "the connection returns a 400" should {

      "respond with an error" in {

        lazy val target = createTarget(HttpResponse(BAD_REQUEST))
        lazy val result = await(target.getEnrolmentsResponse("enrolment-url"))

        lazy val exception = intercept[Exception] {
          await(result)
        }
        exception.getMessage shouldBe s"Failed to retrieve enrolments"
      }
    }
  }
}
