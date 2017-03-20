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

import javax.inject.Inject

import connectors.{DESConnector, GovernmentGatewayConnector, SuccessDesResponse}
import models.SubmissionModel
import play.api.http.Status._


class RelationshipService @Inject()(ggConnector: GovernmentGatewayConnector, desConnector: DESConnector) {

  trait RelationshipResponse
  case object SuccessfulAgentCreation extends RelationshipResponse
  case object FailedAgentCreation extends RelationshipResponse

  def createRelationship(submissionModel: SubmissionModel): Unit = {
    val relationshipModel = submissionModel.relationshipModel
    for {
      ggResponse <- ggConnector.createClientRelationship(submissionModel)
      desResponse <- desConnector.createAgentClientRelationship(relationshipModel)
    } yield (ggResponse, desResponse) match {
      case (NO_CONTENT, SuccessDesResponse) => SuccessfulAgentCreation
      case (_, _) => FailedAgentCreation
    }
  }

}
