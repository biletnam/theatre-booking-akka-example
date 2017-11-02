# Theatre Booking Example #
This is an example application that manages the ticketing and capacity for movie theatres. The purpose of this 
application is to demonstrate different features provided by the [Akka toolkit](https://akka.io) (Clustering, Sharding, 
HTTP) in order to build a robust and scalable system.

## Functionality ##
This application provides a very simple API which allows you to book tickets for a Movie Theatre that has a fixed 
capacity. Using Actors and Cluster Sharding, it is able to prevent users from over-booking a movie theatre.

Theatres are fixed to a seating capacity of 50. 

Here's an example of how to book 10 tickets for the `star-wars` movie theatre:

`POST localhost:8080/theatres/star-wars/tickets/10`

If all goes well, the expected response will be an `HTTP 201 Created` with a body of `10` indicating that 10 tickets 
have been booked. 

If you try and book more than the seating capacity of the theatre. For example:

`POST localhost:8080/theatres/star-wars/tickets/80`

You will receive an `HTTP 403` response with a body telling you `Not Enough Seats`

When the theatre is at capacity (i.e. all tickets have been booked) and you try to book some more:

`POST localhost:8080/theatres/star-wars/tickets/1`

You will receive an `HTTP 403` response with a body telling you `Theatre is at capacity`.

# Technologies #

## Actors ##
Actors are the primary building block in the actor model. An actor is a lightweight process which has only four core 
operations: 
- `Create`:  An actor can create other actors
- `Send`: An actor can only communicate with another actor by sending it messages. 
- `Become`: An actor can change how it handles incoming messages by swapping out its behavior.
- `Supervise`: An actor supervises the actors that it creates. This is essential for handling failures

## Clustering ##
Clustering (specifically Akka Cluster) makes it possible to dynamically grow and shrink the number of nodes used by a 
distributed application, and removes the fear of a single point of failure. Many distributed applications run in 
environments that are not completely under your control, like cloud computing platforms (like AWS, GCP, Azure) located 
across the world. The larger the cluster, the greater the chance of failure. Therefore, it is very important to have 
the means to manage a dynamically shrinking/growing set of nodes which is what Akka Cluster provides you.

Here are a couple of key clustering terms that you should understand:
- `node`: This represents a logical member of a cluster. There can be multiple nodes on a single physical machine, each differentiated by their `hostname`:`port`:`uid` tuple (a node is a cluster enabled actor system).
- `cluster`: This is a set of `node`s joined together through the membership service for the purpose of performing work.
- `leader`: This is a single `node` in a `cluster` that performs certain duties, such as managing cluster convergence and member-state transitions.

Every `cluster` is made up of a set of member `node`s, with each member node being identified by a `hostname`:`port`:`uid` 
tuple. As each actor system comes up, it can specify the `hostname`:`port` part of that identifier via config. Then, 
the Akk Cluster itself will assign a `uid` to that `node` so that it can be uniquely identified within the cluster. 

Once a particular `hostname`:`port`:`uid` tuple joins a cluster, __it can never join again after leaving__. If the 
three-part identifier for a `node` is quarantined—through the remote death watch feature — it can never rejoin the cluster 
again. So, if a `node` shuts down and then comes back up and wants to rejoin the cluster, a new `uid` will be generated for 
that `node`. This will give it a unique hostname:port:uid tuple again, and allow it to join back in.

The nature of the membership state within the cluster is very dynamic. `Node`s are able to join and leave the cluster at 
will, making it so that the membership state is a very fluid thing. Even though the state is dynamic and can be 
constantly changing, every `node` needs to know the current membership state in case it needs to communicate with a 
component within the cluster. In order to deal with this requirement, the Akka team looked to Amazon's Dynamo whitepaper 
for inspiration, using a decentralized __gossip__ protocol-based system to manage membership state throughout the cluster.

The important feature of this membership management system is that there is no master of information. It's not like the 
leader holds and manages the current state and then broadcasts it to the cluster. The state itself is managed within 
each member `node`, and the `node`s then gossip the current state information randomly throughout the cluster, with a 
preference to `node`s that have not seen the latest membership state version.

As different changes can happen concurrently (where `node` A can come up and `node` B can go down at the same time), the 
state data in each `node` uses a data structure called a __vector clock__ to reconcile and merge state differences on 
the information being gossiped throughout the cluster. As `node`s are seeing the latest state, they are modifying the 
gossiped information to indicate that they have seen those changes. __Gossip convergence__ occurs when a `node` can 
prove that the membership-state information he is seeing is also seen by all other `node`s in the cluster. When 
convergence has occurred, the member-state information is consistent throughout the `node`s in the cluster.

Once the cluster has achieved gossip convergence, a `leader` can be determined. This is not an election process though, 
as you may see in other cluster-based systems. Selecting the leader is a deterministic process that can be conducted by 
each node once convergence has occurred. At that point, each `node` just selects the first sorted `node` in the list of 
members that is able to fulfill this role and selects it as the `leader`. As this list is the same in all `node`s, via 
convergence, every `node` deterministically selects the same `node` to be the `leader` with no voting process and/or 
quorum required. Additionally, the `leader` can change at each new point of convergence. This is because the list of 
`node`s changes and so the first sorted `node` may change as well.

The __`leader`__ is the `node` responsible to shift members in and out of the cluster. If a new `node` comes into the 
cluster, the `leader` is the one responsible for changing it from the joining state into the up state. Conversely, if a 
`node` is exiting the cluster, the `leader` will shift it from the exiting state into the removed state. The `leader` 
also can auto-down `node`s, if configured to do so, if the failure detector has indicated that the `node` is unreachable 
after a certain amount of time.

![image](https://user-images.githubusercontent.com/14280155/32307189-97ef1e02-bf55-11e7-9e82-2fc2435246f5.png)

Within the cluster, a sophisticated failure detector is employed to continually monitor whether or not the members are 
currently reachable. This system is based on The Phi Accrual Failure Detector. In this model, instead of a simple yes/no 
answer for whether a node is reachable, you get a phi score, which is based on a history of failure statistics that are 
kept over time. If that phi score goes over a configured threshold, then the `node` will be considered down.

## Cluster Sharding ##

### Sharding in general ###
Sharding is a powerful technique that makes it possible to scale and distribute databases, while keeping them consistent. 
The basic idea is that every record in your database has a shard key attached to it. This shard key is used to determine 
the distribution of data. Rather than storing all records for the database on a single node in the cluster, the records 
are distributed according to this shard key. 

In the example below, the shard key is `s_no`

![image](https://user-images.githubusercontent.com/14280155/32305059-80098ec8-bf49-11e7-9067-9d42b399b34b.png)

### Akka Cluster Sharding ###
Rather than sharding the data, __you shard live actors across the cluster__. Each actor is assigned an entity ID. This 
ID is unique within the cluster. It usually represents the identifier for the domain entity that the actor is modeling. 
You provide a function that will extract that entity ID from the message being sent. 

```scala
case class MovieTheatreEnvelope(id: String, command: MovieTheatre.Command)

val extractEntityId: ShardRegion.ExtractEntityId = {
  case e: MovieTheatreEnvelope => (e.id, e.command)
}
```

In the application, we choose the theatre name to be the entity ID and shard Movie Theatre Actors based on this ID.

You also provide a function that takes the message and computes the ID for the shard on which the actor will reside.
```scala
// numberOfShards is taken from the application.conf
def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
  case e: MovieTheatreEnvelope => (Math.abs(e.id.hashCode) % numberOfShards).toString
  case ShardRegion.StartEntity(id) => (id.toLong           % numberOfShards).toString
}
```

When a message is sent, the aforementioned functions are applied to locate the appropriate shard using the shard ID, and 
then to locate the actor within that shard using the entity ID. 

![image](https://user-images.githubusercontent.com/14280155/32305810-ef5bd94e-bf4d-11e7-9cd2-c933785f99dc.png)

Each entity shown here represents a Movie Theatre Actor.

This makes it possible for you to locate the unique instance of that actor within the cluster. 
In the event that no actor currently exists, the actor will be created. 

__The sharding mechanism will ensure that only one instance of each actor (singleton semantics) exists in the cluster 
at any time.__

__Shards__ are distributed across the cluster in what are known as __shard regions__. These regions act as hosts for the 
shards. Each node that participates in sharding will host a single shard region for each type of sharded actor. Each 
region can in turn host multiple shards. All entities within a shard region are represented by the same type of actor.

![image](https://user-images.githubusercontent.com/14280155/32306350-255d06a0-bf51-11e7-85bf-50a741d5b7d1.png)

Each individual shard can host many entities. These entities are distributed across the shards according to the 
computation of the shard ID that you provided.

Internally, a __shard coordinator__ is used to manage where the shards are located. The coordinator informs shard 
regions as to the location of individual shards. Those shard regions can in turn be used to send a message to the 
entities hosted by the shards. This coordinator is implemented as a 
[cluster singleton](https://doc.akka.io/docs/akka/current/scala/cluster-singleton.html#cluster-singleton). However, its 
interaction within the actual message flow is minimized. It only participates in the messaging if the location of the 
shard is not known. 

![image](https://user-images.githubusercontent.com/14280155/32306466-cdf05c4a-bf51-11e7-8abf-2a46588571d9.png)


In this case, the shard region can communicate with the __shard coordinator__ to locate the shard. That information is 
then cached. Going forward, messages can be sent directly _without_ the need to communicate with the coordinator.

### A note on consistency ###
With a basic understanding of how sharding works, __How does it help with consistency specifically?__

Sharding provides a __consistency boundary__ in the form of a __*single consistent actor*__ in the cluster. All 
communication that is done with a specific entity ID always goes through that actor. The Akka cluster sharding mechanism 
ensures that only one actor with the given ID is running in the cluster at any time. This means that you can use the 
single-threaded illusion to your advantage. 
- You know that any requests will always go to that particular actor 
- You also know that an actor can process only one message at a time. 

This means that you can _guarantee_ __message ordering__ and __consistent state__ within the bounds of that actor.

By creating a boundary around the consistency and isolating it to a single entity, you can use that boundary to allow 
scalability. Consistency is limited to a single entity, and therefore you can scale the system across multiple entities.
