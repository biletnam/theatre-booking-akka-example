# Theatre Booking Example #
This is an example application that manages the ticketing and capacity for movie theatres. The purpose of this 
application is to demonstrate different features provided by the Akka toolkit (Clustering, Sharding, HTTP) in order
to build a robust and scalable system.

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


This makes it possible for you to locate the unique instance of that actor within the cluster. 
In the event that no actor currently exists, the actor will be created. 

__The sharding mechanism will ensure that only one instance of each actor (singleton semantics) exists in the cluster 
at any time.__

