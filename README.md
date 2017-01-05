# akka-cqrs-activator

###Issue tracking demo application which shows implementation of event sourcing and CQRS with Akka

General concept:

- Cassandra is being used as an event store and also as a "read side" data store
- Akka Persistent FSM is being used on "write side" to store current state and to define behavior.
One persistent actor per "issue" is being used
- After events are being persisted into Cassandra database, 
 they're being polled by a "read side" (Akka Persistence Query),
 read store is being updated and then, they are being published via pub-sub mediator actor to the HTTP server.
- To deal with eventual consistency on the UI application, all events are being streamed as 
"a server sent events" from HTTP server. Therefore, UI application can access to "historical" data by simple 
querying of read store, and also to subscribe to the stream if incoming events and use that as an advantage
to live-update its UI.
