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

package controllers

import audit.Logging
import auth.AuthorisedActions
import common.Keys.{EnrolmentKeys => keys}
import config.ApplicationConfig
import models._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import play.api.mvc.Results._
import play.api.test.FakeRequest
import services._
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class RelationshipControllerSpec extends UnitSpec with MockitoSugar with OneAppPerSuite with BeforeAndAfter {

  val mockLoggingUtils: Logging = mock[Logging]
  lazy val mockAppConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  implicit val ec: ExecutionContext = mock[ExecutionContext]

  def createMockAuthService(authServiceResponse: Option[AuthorityModel]): AuthorisationService = {

    val mockAuthService = mock[AuthorisationService]

    authServiceResponse match {
      case Some(data) =>
        when(mockAuthService.getUserAuthority()(ArgumentMatchers.any()))
          .thenReturn(Future.successful(data))
      case _ =>
        when(mockAuthService.getUserAuthority()(ArgumentMatchers.any()))
          .thenReturn(Future.failed(new Exception("Dummy exception")))
    }
    mockAuthService
  }

  def createMockRelationshipService(expectedRelationshipResponse: RelationshipResponse): RelationshipService = {

    val mockRelationshipService: RelationshipService = mock[RelationshipService]

    when(mockRelationshipService.createRelationship(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(expectedRelationshipResponse))

    mockRelationshipService
  }


  "Calling .createRelationship action" when {

    "the model submitted can be parsed to a Submission Model" when {

      val submissionModel = SubmissionModel(mock[RelationshipModel], "")
      val request = FakeRequest().withJsonBody(Json.toJson(submissionModel))

      "the user is authorised" when {

        "the relationship service returns a SuccessfulAgentCreationResponse" should {

          "return a status of 204" in {

            lazy val authService = createMockAuthService(
              Some(AuthorityModel("Agent", Set(Enrolment(keys.agentEnrolmentKey, Seq(Identifier("DummyKey", "DummyValue")), ""))))
            )
            lazy val actions = new AuthorisedActions(authService)
            lazy val relationshipService = createMockRelationshipService(SuccessfulAgentCreation)
            lazy val controller = new RelationshipController(actions, relationshipService)

            lazy val result = await(controller.createRelationship(request))
            status(result) shouldBe 204
          }
        }

        "the relationship service returns a FailedAgentCreationResponse/throws an exception" should {

          "return a status of 500" in {
            lazy val authService = createMockAuthService(
              Some(AuthorityModel("Agent", Set(Enrolment(keys.agentEnrolmentKey, Seq(Identifier("DummyKey", "DummyValue")), ""))))
            )
            lazy val actions = new AuthorisedActions(authService)
            lazy val relationshipService = createMockRelationshipService(FailedAgentCreation)
            lazy val controller = new RelationshipController(actions, relationshipService)

            lazy val result = await(controller.createRelationship(request))
            status(result) shouldBe 500
          }
        }
      }

      "the user is not authorised" should {

        "return a status of 401" in {
          lazy val authService = createMockAuthService(
            Some(AuthorityModel("Not an Agent", Set(Enrolment("Not the enrolmentKey", Seq(Identifier("DummyKey", "DummyValue")), ""))))
          )
          lazy val actions = new AuthorisedActions(authService)
          lazy val relationshipService = createMockRelationshipService(FailedAgentCreation)
          lazy val controller = new RelationshipController(actions, relationshipService)

          lazy val result = await(controller.createRelationship(request))
          status(result) shouldBe 401
        }
      }
    }

    "the model submitted cannot be parsed into a SubmissionModel" should {

      val submissionModel = "Not a submission model"
      val request = FakeRequest().withJsonBody(Json.toJson(submissionModel))

      "return a status of 404" in {
        lazy val authService = createMockAuthService(
          Some(AuthorityModel("Agent", Set(Enrolment(keys.agentEnrolmentKey, Seq(Identifier("DummyKey", "DummyValue")), ""))))
        )
        lazy val actions = new AuthorisedActions(authService)
        lazy val relationshipService = createMockRelationshipService(FailedAgentCreation)
        lazy val controller = new RelationshipController(actions, relationshipService)

        lazy val result = await(controller.createRelationship(request))
        status(result) shouldBe 400
      }
    }
  }
}
