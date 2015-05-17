/**
 * Created by henry on 5/17/15.
 */
import akka.actor.{ ActorRef, ActorSystem }
import akka.contrib.pattern.ClusterSharding

object ClusterBoot {

  def boot(proxyOnly: Boolean = false)(clusterSystem: ActorSystem): (ActorRef, ActorRef) = {

    val userListing = ClusterSharding(clusterSystem).start(
      typeName = UserListing.shardName,
      entryProps = if (proxyOnly) None else Some(UserListing.props),
      idExtractor = UserListing.idExtractor,
      shardResolver = UserListing.shardResolver)

    val permissionRepo = ClusterSharding(clusterSystem).start(
      typeName = PermissionRepo.shardName,
      entryProps = if (proxyOnly) None else Some(PermissionRepo.props(userListing)),
      idExtractor = PermissionRepo.idExtractor,
      shardResolver = PermissionRepo.shardResolver)

    (permissionRepo, userListing)
  }
}
