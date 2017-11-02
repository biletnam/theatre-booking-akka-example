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

![image](https://user-images.githubusercontent.com/14280155/32329478-986016da-bfb3-11e7-94ef-912696a6825d.png)

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

Here is the general flow when communicating with a shard region to accesses sharded entity actors:

![image](https://user-images.githubusercontent.com/14280155/32307598-a7da2d14-bf57-11e7-9327-a4473bcfe6c0.png)

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
