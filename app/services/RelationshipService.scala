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

import javax.inject.{Inject, Singleton}

import config.WSHttp
import connectors.{DESConnector, GovernmentGatewayConnector, SuccessDesResponse}
import models.{RelationshipModel, SubmissionModel}
import play.api.http.Status._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

trait RelationshipResponse
case object SuccessfulAgentCreation extends RelationshipResponse
case object FailedAgentCreation extends RelationshipResponse
@Singleton
class RelationshipService @Inject()(ggConnector: GovernmentGatewayConnector, desConnector: DESConnector){

  val http: HttpGet = WSHttp
  
  def createRelationship(submissionModel: SubmissionModel)(implicit hc: HeaderCarrier): Future[RelationshipResponse] = {
    val relationshipModel = submissionModel.relationshipModel
    for {
      ggResponse <- ggConnector.createClientRelationship(submissionModel)
      desResponse <- desConnector.createAgentClientRelationship(relationshipModel)
    } yield (ggResponse, desResponse) match {
      case (NO_CONTENT, SuccessDesResponse) => SuccessfulAgentCreation
      case (_, _) => FailedAgentCreation
    }
  }

  def createGgRelationship(submissionModel: SubmissionModel)(implicit hc: HeaderCarrier): Future[RelationshipResponse] = {
    ggConnector.createClientRelationship(submissionModel) map {
      result =>
        if (result == NO_CONTENT)
          SuccessfulAgentCreation
        else
          FailedAgentCreation
    }
  }

  def createDesRelationship(relationshipModel: RelationshipModel)(implicit hc: HeaderCarrier): Future[RelationshipResponse] = {
    desConnector.createAgentClientRelationship(relationshipModel) map {
      result =>
        if (result == SuccessDesResponse)
          SuccessfulAgentCreation
        else
          FailedAgentCreation
    }
  }
}
