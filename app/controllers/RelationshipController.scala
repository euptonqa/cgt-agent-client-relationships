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

import javax.inject.{Inject, Singleton}

import models.SubmissionModel
import play.api.mvc.{Action, Result}
import services.{RelationshipResponse, RelationshipService, SuccessfulAgentCreation}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class RelationshipController @Inject()(relationshipService: RelationshipService) extends BaseController {

  val createRelationship = Action.async {
    implicit request =>
        val model = request.body.asJson.get.as[SubmissionModel]
        val result = relationshipService.createRelationship(model).map { response =>
          mapAgentResponse(response)
        }

      result.recover{
        case _ => InternalServerError
      }
  }

  def mapAgentResponse(relationshipResponse: RelationshipResponse): Result = {
    if (relationshipResponse == SuccessfulAgentCreation) {
      NoContent
    }
    else {
      InternalServerError
    }
  }
}
