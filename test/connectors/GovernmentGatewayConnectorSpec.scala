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

import config.ApplicationConfig
import models.{RelationshipModel, SubmissionModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.test.UnitSpec
import play.api.http.Status._

import scala.concurrent.Future

class GovernmentGatewayConnectorSpec extends UnitSpec with MockitoSugar with OneAppPerSuite {
  val config = app.injector.instanceOf[ApplicationConfig]
  implicit val hc = HeaderCarrier()

  def createMockedConnector(responseCode: Int, responseBody: String): GovernmentGatewayConnector = {
    val mockHttp = mock[WSHttp]
    val mockHttpResponse = mock[HttpResponse]

    when(mockHttpResponse.status)
      .thenReturn(responseCode)

    when(mockHttpResponse.body)
      .thenReturn(responseBody)

    when(mockHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
      ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(mockHttpResponse))

    new GovernmentGatewayConnector(config, mockHttp)
  }

  "Calling .createClientRelationship" should {

    "return a successful future when a 204 response is received" in {
      lazy val connector = createMockedConnector(NO_CONTENT, "")
      lazy val result = connector.createClientRelationship(SubmissionModel(RelationshipModel("arn", "cgtRef"), "name"))

      await(result) shouldBe NO_CONTENT
    }

    "return a failed future when a 500 response is received" in {
      lazy val connector = createMockedConnector(INTERNAL_SERVER_ERROR, "error message")
      lazy val result = connector.createClientRelationship(SubmissionModel(RelationshipModel("arn", "cgtRef"), "name"))
      lazy val exception = intercept[Exception] {
        await(result)
      }

      exception.getMessage shouldBe "Invalid Gateway response code:500 message:error message"
    }

    "return a failed future when a 400 response is received" in {
      lazy val connector = createMockedConnector(BAD_REQUEST, "no service name")
      lazy val result = connector.createClientRelationship(SubmissionModel(RelationshipModel("arn", "cgtRef"), "name"))
      lazy val exception = intercept[Exception] {
        await(result)
      }

      exception.getMessage shouldBe "Invalid Gateway response code:400 message:no service name"
    }

    "return a failed future with an unexpected error response" in {
      lazy val connector = createMockedConnector(REQUEST_TIMEOUT, "no response")
      lazy val result = connector.createClientRelationship(SubmissionModel(RelationshipModel("arn", "cgtRef"), "name"))
      lazy val exception = intercept[Exception] {
        await(result)
      }

      exception.getMessage shouldBe "Invalid Gateway response code:408 message:no response"
    }
  }
}
