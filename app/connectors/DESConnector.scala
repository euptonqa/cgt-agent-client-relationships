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

import javax.inject.{Inject, Singleton}

import audit.Logging
import config.{ApplicationConfig, WSHttp}
import models.RelationshipModel
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.Authorization
import play.api.http.Status._
import common.Constants.AuditConstants._

sealed trait DesResponse

case class SuccessDesResponse(response: JsValue) extends DesResponse

case object NotFoundDesResponse extends DesResponse

case object DesErrorResponse extends DesResponse

case class InvalidDesRequest(message: String) extends DesResponse

case object DuplicateDesResponse extends DesResponse

@Singleton
class DESConnector @Inject()(appConfig: ApplicationConfig, logger: Logging){

  lazy val serviceUrl: String = appConfig.baseUrl("des")
  lazy val serviceContext: String = appConfig.desContextUrl

  val environment = "test"
  val token = "des"

  val urlHeaderEnvironment = "???"
  val urlHeaderAuthorization = "???"

  //TODO: Need to update when known environments and tokens...

  val http: HttpGet with HttpPost with HttpPut = WSHttp

  def createAgentClientRelationship(relationshipModel: RelationshipModel)(implicit hc: HeaderCarrier): Unit ={
    val arnReference = relationshipModel.arn
    Logger.warn(s"Made a POST request to the stub to create a relationship model with the ARN ${arnReference}" +
      s"and CGT Ref ${relationshipModel.cgtRef}")

    val requestUrl: String = s"$serviceUrl$serviceContext/create-relationship/"
    //TODO: Update the correct DES end point is known
    val response = cPOST(requestUrl, Json.toJson(relationshipModel))

    val auditMap: Map[String, String] = Map("ARN"->relationshipModel.arn, "Url"->requestUrl)

    response map {
      r =>
        r.status match {
          case OK =>
            Logger.info(s"Successful DES submission for $arnReference")
            logger.audit(transactionDESRelationshipCreation, auditMap, eventTypeSuccess)
            SuccessDesResponse(r.json)
          case CONFLICT =>
            Logger.warn("Error Conflict: SAP Number already in existence")
            logger.audit(transactionDESRelationshipCreation, conflictAuditMap(auditMap, r), eventTypeConflict)
            DuplicateDesResponse
          case ACCEPTED =>
            Logger.info(s"Accepted DES submission for $arnReference")
            logger.audit(transactionDESRelationshipCreation, auditMap, eventTypeSuccess)
            SuccessDesResponse(r.json)
          case BAD_REQUEST =>
            val message = (r.json \ "reason").as[String]
            Logger.warn(s"Error with the request $message")
            logger.audit(transactionDESRelationshipCreation, failureAuditMap(auditMap, r), eventTypeFailure)
            InvalidDesRequest(message)
        }
    } recover {
      case _: NotFoundException =>
        Logger.warn(s"Not found for $arnReference")
        logger.audit(transactionDESRelationshipCreation, auditMap, eventTypeNotFound)
        NotFoundDesResponse
      case _: InternalServerException =>
        Logger.warn(s"Internal server error for $arnReference")
        logger.audit(transactionDESRelationshipCreation, auditMap, eventTypeInternalServerError)
        DesErrorResponse
      case _: BadGatewayException =>
        Logger.warn(s"Bad gateway status for $arnReference")
        logger.audit(transactionDESRelationshipCreation, auditMap, eventTypeBadGateway)
        DesErrorResponse
      case ex: Exception =>
        Logger.warn(s"Exception of ${ex.toString} for $arnReference")
        logger.audit(transactionDESRelationshipCreation, auditMap, eventTypeGeneric)
        DesErrorResponse
    }
  }


  @inline
  private def cPOST[I, O](url: String, body: I, headers: Seq[(String, String)] = Seq.empty)(implicit wts: Writes[I], rds: HttpReads[O], hc: HeaderCarrier) = {
    http.POST[I, O](url, body, headers)(wts = wts, rds = rds, hc = createHeaderCarrier(hc))
  }

  private def createHeaderCarrier(headerCarrier: HeaderCarrier): HeaderCarrier = {
    headerCarrier.
      withExtraHeaders("Environment" -> urlHeaderEnvironment).
      copy(authorization = Some(Authorization(urlHeaderAuthorization)))
  }

  private def conflictAuditMap(auditMap: Map[String, String], response: HttpResponse) =
    auditMap ++ Map("Conflict reason" -> response.body, "Status" -> response.status.toString)

  private def failureAuditMap(auditMap: Map[String, String], response: HttpResponse) =
    auditMap ++ Map("Failure reason" -> response.body, "Status" -> response.status.toString)
}
