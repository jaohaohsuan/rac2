/**
 * Created by henry on 5/17/15.
 */
import akka.actor.{ PoisonPill, ActorRef, ActorSystem, Props }
import akka.contrib.pattern.{ ClusterSingletonProxy, ClusterSharding, ClusterSingletonManager }

object ClusterBoot {

  def boot(proxyOnly: Boolean = false)(clusterSystem: ActorSystem): (ActorRef, ActorRef, ActorRef) = {

    clusterSystem.actorOf(ClusterSingletonManager.props(
      singletonProps = Props[PathListing],
      singletonName = "pathListing",
      terminationMessage = PoisonPill,
      role = None
    ), "singleton")

    val userListing = ClusterSharding(clusterSystem).start(
      typeName = UserListing.shardName,
      entryProps = if (proxyOnly) None else Some(UserListing.props),
      idExtractor = UserListing.idExtractor,
      shardResolver = UserListing.shardResolver)

    lazy val pathListing = clusterSystem.actorOf(ClusterSingletonProxy.props(
      singletonPath = "/user/singleton/pathListing",
      role = None),
      name = "pathListingProxy")

    val permissionRepo = ClusterSharding(clusterSystem).start(
      typeName = PermissionRepo.shardName,
      entryProps = if (proxyOnly) None else Some(PermissionRepo.props(userListing, pathListing)),
      idExtractor = PermissionRepo.idExtractor,
      shardResolver = PermissionRepo.shardResolver)

    (permissionRepo, userListing, pathListing)
  }
}
