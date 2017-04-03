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
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.Authorization
import play.api.http.Status._
import common.Constants.AuditConstants._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait DesResponse

case object SuccessDesResponse extends DesResponse

case object NotFoundDesResponse extends DesResponse

case object DesErrorResponse extends DesResponse

case object InvalidDesRequest extends DesResponse

case object DuplicateDesResponse extends DesResponse

@Singleton
class DESConnector @Inject()(appConfig: ApplicationConfig, logger: Logging) extends HttpErrorFunctions{

  lazy val serviceUrl: String = appConfig.baseUrl("des")
  lazy val serviceContext: String = appConfig.desContextUrl
  lazy val environment = appConfig.desEnvironment
  lazy val token = appConfig.desToken

  val http: HttpGet with HttpPost with HttpPut = WSHttp

  def createAgentClientRelationship(relationshipModel: RelationshipModel)(implicit hc: HeaderCarrier): Future[DesResponse] ={
    val arnReference = relationshipModel.arn
    Logger.warn(s"Made a POST request to the stub to create a relationship model with the ARN $arnReference" +
      s" and CGT Ref ${relationshipModel.cgtRef}")
    val requestUrl: String = s"$serviceUrl$serviceContext/create-relationship/"
    val response = cPOST(requestUrl, Json.toJson(relationshipModel))
    val auditMap: Map[String, String] = Map("ARN" -> arnReference, "Url" -> requestUrl)
    response map {
      r =>
        r.status match {
          case OK =>
            Logger.info(s"Successful DES submission for $arnReference")
            logger.audit(transactionDESRelationshipCreation, auditMap, eventTypeSuccess)
            SuccessDesResponse
          case CONFLICT =>
            Logger.warn("Error Conflict: SAP Number already in existence")
            logger.audit(transactionDESRelationshipCreation, conflictAuditMap(auditMap, r), eventTypeConflict)
            DuplicateDesResponse
          case ACCEPTED =>
            Logger.info(s"Accepted DES submission for $arnReference")
            logger.audit(transactionDESRelationshipCreation, auditMap, eventTypeSuccess)
            SuccessDesResponse
          case NO_CONTENT =>
            Logger.info(s"Accepted DES submission for $arnReference")
            logger.audit(transactionDESRelationshipCreation, auditMap, eventTypeSuccess)
            SuccessDesResponse
          case BAD_REQUEST =>
            Logger.warn(s"Error with the request ${r.body}")
            logger.audit(transactionDESRelationshipCreation, failureAuditMap(auditMap, r), eventTypeFailure)
            InvalidDesRequest
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
    headerCarrier.copy(authorization = Some(Authorization(s"Bearer $token"))).withExtraHeaders("Environment" -> environment)
  }

  private def conflictAuditMap(auditMap: Map[String, String], response: HttpResponse) =
    auditMap ++ Map("Conflict reason" -> response.body, "Status" -> response.status.toString)

  private def failureAuditMap(auditMap: Map[String, String], response: HttpResponse) =
    auditMap ++ Map("Failure reason" -> response.body, "Status" -> response.status.toString)

  implicit val httpRds = new HttpReads[HttpResponse] {
    def read(http: String, url: String, res: HttpResponse): HttpResponse = customDESRead(http, url, res)
  }

  private[connectors] def customDESRead(http: String, url: String, response: HttpResponse) = {
    response.status match {
      case BAD_REQUEST => response
      case NOT_FOUND => throw new NotFoundException("DES returned a Not Found status")
      case CONFLICT => response
      case INTERNAL_SERVER_ERROR => throw new InternalServerException("DES returned an internal server error")
      case BAD_GATEWAY => throw new BadGatewayException("DES returned an upstream error")
      case _ => handleResponse(http, url)(response)
    }
  }
}
