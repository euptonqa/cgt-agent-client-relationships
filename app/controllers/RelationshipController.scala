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

import auth.AuthorisedActions
import models.{ExceptionResponse, SubmissionModel}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import services.{RelationshipResponse, RelationshipService, SuccessfulAgentCreation}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class RelationshipController @Inject()(actions: AuthorisedActions,
                                       relationshipService: RelationshipService) extends BaseController {

  private val unauthorisedAction: Future[Result] = Future.successful(Unauthorized(Json.toJson(ExceptionResponse(UNAUTHORIZED, "Unauthorised"))))
  private val badRequest: Future[Result] = Future.successful(BadRequest(Json.toJson(ExceptionResponse(BAD_REQUEST, "Bad Request"))))

  val createRelationship: Action[AnyContent] = Action.async {
    implicit request =>

      Try(request.body.asJson.get.as[SubmissionModel]) match {
        case Success(model) =>

          def mapAgentResponse(relationshipResponse: RelationshipResponse): Result = {
            if (relationshipResponse == SuccessfulAgentCreation) {
              NoContent
            }
            else {
              InternalServerError
            }
          }

          actions.authorisedAgentAction {
            case true => relationshipService.createRelationship(model).map { response =>
              mapAgentResponse(response)
            }
            case false => unauthorisedAction
          }
        case Failure(_) => badRequest
      }
  }
}
