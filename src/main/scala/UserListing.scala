/**
 * Created by henry on 5/17/15.
 */

import akka.contrib.pattern.ShardRegion
import akka.actor._

import PermissionRepo._

object UserListing {

  case class GetUsers(path: String)

  def props = Props[UserListing]

  val shardName = "userListing"

  val idExtractor: ShardRegion.IdExtractor = {
    case m @ GetUsers(path) => (path.take(1), m)
    case e: Appended => (e.path.take(1), e)
  }

  val shardResolver: ShardRegion.ShardResolver = {
    case GetUsers(path) => math.abs(path.take(1).hashCode) % 100 toString
    case e: Appended => math.abs(e.path.take(1).hashCode) % 100 toString
  }

}

class UserListing extends Actor {

  import UserListing._

  var state: Map[String, Map[String, (String, Long)]] = Map.empty

  val receive: Receive = {
    case Appended(user, path, operation, dueDate) =>
      state += (path -> state.getOrElse(user, Map.empty).+(user -> (operation, dueDate)))
    case GetUsers(path) =>
      sender() ! state.getOrElse(path, Map.empty)
  }

}