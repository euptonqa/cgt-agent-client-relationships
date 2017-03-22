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

import audit.Logging
import config.ApplicationConfig
import connectors.{DESConnector, GovernmentGatewayConnector}
import models.{RelationshipModel, SubmissionModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class RelationshipServiceSpec extends UnitSpec with OneAppPerSuite with MockitoSugar with BeforeAndAfter {

  val mockHttpGG = mock[WSHttp]
  val mockHttpDES = mock[WSHttp]
  val mockLoggingUtils: Logging = mock[Logging]
  lazy val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val ec = mock[ExecutionContext]
  implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  object GGConnector extends GovernmentGatewayConnector(mockAppConfig, mockHttpGG)

  object TestDESConnector extends DESConnector(mockAppConfig, mockLoggingUtils) {
    override lazy val serviceUrl = "test"
    override val environment = "test"
    override val token = "test"
    override val http: WSHttp = mockHttpDES
    override val urlHeaderEnvironment = "??? see srcs, found in config"
    override val urlHeaderAuthorization = "??? same as above"
  }

  lazy val relationshipService = new RelationshipService(GGConnector, TestDESConnector)(hc)

  before {
    reset(mockHttpDES)
    reset(mockHttpGG)
  }

  "Calling .createAgentRelationship" when {
    "Both the GGConnector and DESConnector return successful responses" in {
      val submissionModel = SubmissionModel(mock[RelationshipModel], "")

      when(mockHttpGG.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(responseStatus = 204)))

      when(mockHttpDES.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(responseStatus = 204)))

      val result = await(relationshipService.createRelationship(submissionModel))

      result shouldBe SuccessfulAgentCreation
    }

    "One connector returns a failed response" should {
      "throw an exception" in {
        val submissionModel = SubmissionModel(mock[RelationshipModel], "")

        when(mockHttpGG.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = 500)))

        when(mockHttpDES.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = 204)))

        val result = relationshipService.createRelationship(submissionModel)

        lazy val exception = intercept[Exception] {
          await(result)
        }

        exception.getMessage shouldBe "Invalid Gateway response code:500 message:null"
      }
    }

    "Both connectors return a failed response" should {
      "throw an exception" in {
        val submissionModel = SubmissionModel(mock[RelationshipModel], "")

        when(mockHttpGG.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = 500)))

        when(mockHttpDES.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(),
          ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = 500)))

        val result = relationshipService.createRelationship(submissionModel)

        lazy val exception = intercept[Exception] {
          await(result)
        }

        exception.getMessage shouldBe "Invalid Gateway response code:500 message:null"
      }
    }
  }
}
