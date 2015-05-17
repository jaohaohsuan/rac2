/**
 * Created by henry on 5/17/15.
 */
import akka.actor.ActorSystem
import com.typesafe.config._

object ClusterNodeApp extends App {

  val conf =
    """akka.remote.netty.tcp.hostname="%hostname%"
       akka.remote.netty.tcp.port=%port%
    """.stripMargin

  val argumentsError = """Please run the service with the required arguments: <hostIpAddress> <port>"""

  assert(args.length == 2, argumentsError)

  val hostname = args(0)
  val port = args(1).toInt

  val config =
    ConfigFactory.parseString(conf.replaceAll("%hostname%", hostname).replaceAll("%port%", port.toString)).withFallback(ConfigFactory.load())

  implicit val clusterSystem = ActorSystem("ClusterSystem", config)

  ClusterBoot.boot()(clusterSystem)
}
