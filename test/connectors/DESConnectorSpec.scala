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

import java.util.UUID

import audit.Logging
import config.ApplicationConfig
import models.RelationshipModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost, WSPut}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class DESConnectorSpec extends UnitSpec with OneServerPerSuite with MockitoSugar with BeforeAndAfter {

  val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  val mockLoggingUtils = mock[Logging]
  val mockWSHttp: MockHttp = mock[MockHttp]
  implicit val hc = mock[HeaderCarrier]
  implicit val ec = mock[ExecutionContext]

  class MockHttp extends WSGet with WSPost with WSPut with HttpAuditing {
    override val hooks = Seq(AuditingHook)

    override def appName: String = "test"

    override def auditConnector: AuditConnector = mock[AuditConnector]
  }

  object TestDESConnector extends DESConnector(mockAppConfig, mockLoggingUtils) {
    val nino: String =  new Generator(new Random()).nextNino.nino.replaceFirst("MA", "AA")
    override val http: MockHttp = mockWSHttp
  }

  before {
    reset(mockWSHttp)
  }

  "httpRds" should {

    "return the http response when a OK status code is read from the http response" in {
      val response = HttpResponse(OK)
      TestDESConnector.httpRds.read("http://", "testUrl", response) shouldBe response
    }

    "return a not found exception when it reads a NOT_FOUND status code from the http response" in {
      intercept[NotFoundException] {
        TestDESConnector.httpRds.read("http://", "testUrl", HttpResponse(NOT_FOUND))
      }
    }
  }

  "Calling .createAgentClientRelationship" should {
    implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

  }

  "Calling .createAgentClientRelationship" should {

    val validRelationshipModel = mock[RelationshipModel]

    implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))

    "return success with an OK" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(OK)))

      val result = await(TestDESConnector.createAgentClientRelationship(validRelationshipModel))

      result shouldBe SuccessDesResponse
    }

    "return duplicated des response with a CONFLICT" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(CONFLICT)))

      val result = await(TestDESConnector.createAgentClientRelationship(validRelationshipModel))

      result shouldBe DuplicateDesResponse
    }

    "return success des response with an ACCEPTED" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(ACCEPTED)))

      val result = await(TestDESConnector.createAgentClientRelationship(validRelationshipModel))

      result shouldBe SuccessDesResponse
    }

    "return success des response with a NO_CONTENT" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      val result = await(TestDESConnector.createAgentClientRelationship(validRelationshipModel))

      result shouldBe SuccessDesResponse
    }

    "return an invalid request with a BAD_REQUEST with the appropriate error message" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_REQUEST)))

      val result = await(TestDESConnector.createAgentClientRelationship(validRelationshipModel))

      result shouldBe InvalidDesRequest
    }

    "return a DesErrorResponse when a NOT_FOUND is sent" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(NOT_FOUND)))

      val result = await(TestDESConnector.createAgentClientRelationship(validRelationshipModel))

      result shouldBe DesErrorResponse
    }

    "return a DesErrorResponse when receiving a INTERNAL_SERVER_ERROR" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR)))

      val result = await(TestDESConnector.createAgentClientRelationship(validRelationshipModel))

      result shouldBe DesErrorResponse
    }

    "return a DesErrorResponse when receiving a BAD_GATEWAY_EXCEPTION" in {
      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(BAD_GATEWAY)))

      val result = await(TestDESConnector.createAgentClientRelationship(validRelationshipModel))

      result shouldBe DesErrorResponse
    }

    "return a DesErrorResponse when a SERVICE_UNAVAILABLE is sent" in {

      when(mockWSHttp.POST[JsValue, HttpResponse](ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
        (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(HttpResponse(SERVICE_UNAVAILABLE)))

      val result = await(TestDESConnector.createAgentClientRelationship(validRelationshipModel))

      result shouldBe DesErrorResponse
    }

  }


}
