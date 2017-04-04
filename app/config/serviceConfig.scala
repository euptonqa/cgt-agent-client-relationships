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

package config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.config.ServicesConfig

trait AppConfig {
  val assetsPrefix: String
  val analyticsToken: String
  val analyticsHost: String
  val desContextUrl: String
  val authBaseUrl: String
  val authAuthorityUrl: String
  val governmentGatewayServiceUrl: String
  val desEnvironment: String
  val desToken: String
}

@Singleton
class ApplicationConfig @Inject()(configuration: Configuration) extends AppConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  override lazy val assetsPrefix: String = loadConfig(s"assets.url") + loadConfig(s"assets.version")
  override lazy val analyticsToken: String = loadConfig(s"google-analytics.token")
  override lazy val analyticsHost: String = loadConfig(s"google-analytics.host")

  override val desContextUrl: String = loadConfig("microservice.services.des.context")
  override lazy val authBaseUrl: String = baseUrl("auth")
  override lazy val authAuthorityUrl: String = s"$authBaseUrl/auth/authority"
  override lazy val governmentGatewayServiceUrl: String = baseUrl("government-gateway")
  override val desEnvironment: String = loadConfig("microservice.services.des.environment")
  override val desToken: String = loadConfig("microservice.services.des.token")
}
