Simple Movie Reservation System
=========================

Goal to create simple movie reservation system using Scala programming language.

### Implementation:
This project is a simple web service based on Akka HTTP and Akka Persistence which implements Event Sourcing and CQRS patterns.

Akka persistence extension comes with a “leveldb” journal plugin, which writes to the local filesystem.
To clean event journal use command:
```
sbt clean
```

## Requirements
* JDK 8
* sbt

## Build Application 

To compile and run tests use following command:

```
sbt compile test
```

For tests Akka persistence actor uses in-memory journaling.

## Run Application 

To run application use following command:

```
sbt run
```

Application creates following Akka HTTP routes

 - HTTP POST http://localhost:9000/register-movie
 - HTTP POST http://localhost:9000/reserve-seat
 - HTTP GET http://localhost:9000/movie-info/$imdbId/$screenId

## Postman Code Snippets

Register a movie:

```
POST /register-movie HTTP/1.1
Host: localhost:9000
Content-Type: application/json
Cache-Control: no-cache

 {
        "imdbId": "tt0111161",
        "availableSeats": 3,
        "screenId": "screen_123456"
 }    
```
 
Reserve a seat:  
 
```
POST /reserve-seat HTTP/1.1
Host: localhost:9000
Content-Type: application/json
Cache-Control: no-cache

  {
        "imdbId": "tt0111161",
        "screenId": "screen_123456"
  }
```     

Retrieve information about the movie:

```
GET /movie-info/tt0111161/screen_123456 HTTP/1.1
Host: localhost:9000
Content-Type: application/json
Cache-Control: no-cache
```