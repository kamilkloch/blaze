package org.http4s.client.blaze

import java.security.{NoSuchAlgorithmException, SecureRandom}
import java.security.cert.X509Certificate
import javax.net.ssl.{SSLContext, X509TrustManager}
import java.util.concurrent._

import org.http4s.BuildInfo
import org.http4s.headers.{AgentProduct, `User-Agent`}
import org.http4s.blaze.util.TickWheelExecutor
import org.http4s.client.impl.DefaultExecutor
import org.http4s.util.threads

import scala.concurrent.duration._
import scala.math.max
import fs2.Task
import fs2.Strategy

private[blaze] object bits {
  // Some default objects
  val DefaultTimeout: Duration = 60.seconds
  val DefaultBufferSize: Int = 8*1024
  val DefaultUserAgent = Some(`User-Agent`(AgentProduct("http4s-blaze", Some(BuildInfo.version))))

  val ClientTickWheel = new TickWheelExecutor()



  def getExecutor(config: BlazeClientConfig): (ExecutorService, Task[Unit]) = config.customExecutor match {
    case Some(exec) => (exec, Task.now(()))
    case None =>
      val exec = DefaultExecutor.newClientDefaultExecutorService("blaze-client")
      implicit val strategy = Strategy.fromExecutor(exec)
      (exec, Task { exec.shutdown() })
  }

  /** The sslContext which will generate SSL engines for the pipeline
    * Override to provide more specific SSL managers */
  lazy val sslContext = defaultTrustManagerSSLContext()

  private class DefaultTrustManager extends X509TrustManager {
    def getAcceptedIssuers(): Array[X509Certificate] =  new Array[java.security.cert.X509Certificate](0)
    def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit = {}
    def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit = {}
  }

  private def defaultTrustManagerSSLContext(): SSLContext = try {
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, Array(new DefaultTrustManager()), new SecureRandom())
    sslContext
  } catch {
    case e: NoSuchAlgorithmException => throw new ExceptionInInitializerError(e)
    case e: ExceptionInInitializerError => throw new ExceptionInInitializerError(e)
  }
}
