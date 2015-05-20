/**
 * Created by henry on 5/17/15.
 */
import akka.actor._
import akka.persistence.journal.leveldb.{ SharedLeveldbStore, SharedLeveldbJournal }
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
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

  startupSharedJournal(clusterSystem, startStore = (port == 2551), path =
    ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store"))

  ClusterBoot.boot()(clusterSystem)

  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath) = {

    if (startStore)
      system.actorOf(Props[SharedLeveldbStore], "store")

    import system.dispatcher
    implicit val timeout = Timeout(15.seconds)

    val f = (system.actorSelection(path) ? Identify(None))
    f.onSuccess {
      case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
      case _ =>
        system.log.error("Shared journal not started at {}", path)
    }
    f.onFailure {
      case _ =>
        system.log.error("Lookup of shared journal at {} time out", path)
        system.shutdown()
    }
  }
}
