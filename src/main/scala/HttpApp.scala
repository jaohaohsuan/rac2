/**
 * Created by henry on 5/17/15.
 */
import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.pattern.{ ask }
import spray.routing._
import spray.can.Http
import com.typesafe.config.ConfigFactory
import akka.io.IO
import akka.util.Timeout
import akka.contrib.pattern.{ ClusterSingletonProxy }

import scala.concurrent.duration._

class ServiceActor(permissionRepo: ActorRef, userListing: ActorRef, pathListing: ActorRef) extends HttpServiceActor
    with HttpPermissionServiceRoute
    with HttpQueryTemplateServiceRoute {

  implicit val executionContext = context.dispatcher

  def receive = runRoute(permissionRoute(permissionRepo, userListing, pathListing) ~ queryTemplateRoute)
}

object HttpApp extends App {

  val argumentsError = """
    Please run the service with the required arguments: "<httpIpAddress>" <httpPort> "<akkaHostIpAddress>" <akkaPort>
                       """

  val conf =
    """akka.remote.netty.tcp.hostname="%hostname%"
       akka.remote.netty.tcp.port=%port%
    """.stripMargin

  assert(args.length == 4, argumentsError)

  val httpHost = args(0)
  val httpPort = args(1).toInt

  val akkaHost = args(2)
  val akkaPort = args(3).toInt

  val config =
    ConfigFactory.parseString(conf.replaceAll("%hostname%", akkaHost)
      .replaceAll("%port%", akkaPort.toString)).withFallback(ConfigFactory.load())

  implicit val system = ActorSystem("ClusterSystem", config)

  val (permissionRepo, userListing, pathListing) = ClusterBoot.boot(true)(system)

  val service = system.actorOf(Props(classOf[ServiceActor], permissionRepo, userListing, pathListing), "http-actor")

  implicit val timeout = Timeout(5.seconds)

  IO(Http) ? Http.Bind(service, interface = httpHost, port = httpPort)

}

