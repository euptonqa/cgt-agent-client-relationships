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

import config.{AppConfig, WSHttp}
import models.{AuthAuthorityModel, Enrolment}
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class AuthorisationConnector @Inject()(config: AppConfig) {

  val http: HttpGet = WSHttp
  val authUrl: String = s"${config.authBaseUrl}"
  val requestUrl = s"""${config.authAuthorityUrl}"""

  def getAuthority()(implicit hc: HeaderCarrier): Future[AuthAuthorityModel] = {

    http.GET[HttpResponse](requestUrl).map {
      response =>
        response.status match {
          case OK =>
            Logger.info("Received an OK response from auth/authority")
            val affinityGroup = (response.json \ "affinityGroup").as[String]
            val enrolmentUrl = (response.json \ "enrolments").as[String]
            AuthAuthorityModel(affinityGroup, enrolmentUrl)
          case e =>
            Logger.warn(s"Received an $e response from auth/authority")
            throw new Exception(s"The request for the authority returned a $e")
        }
    }
  }

  def getEnrolmentsResponse(enrolmentsUrl: String)(implicit hc: HeaderCarrier): Future[Set[Enrolment]] = {
    val getUrl = s"$authUrl$enrolmentsUrl"
    http.GET[HttpResponse](getUrl).map {
      response =>
        response.status match {
          case OK =>
            println(response.json)
            response.json.as[Set[Enrolment]]
          case _ => throw new Exception("Failed to retrieve enrolments")
        }
    }
  }
}
