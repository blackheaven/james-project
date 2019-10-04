# 5. Distributed Task termination ackowledgement

Date: 2019-10-02

## Status

Accepted (lazy consensus)

## Context

By switching the task manager to a distributed implementation, we need to be able to execute a `Task` on any node of the cluster.
We need to have a way to broadcast some `Event`s (`CancelRequested` and termination `Event`s).

## Decision

 * Creating a `RabbitMQEventHandler` which publish `Event`s pushed to the task manager's event system to RabbitMQ
 * All the events which end a `Task` (`Completed`, `Failed`, and `Canceled`) have to be transmitted to other nodes

## Consequences

 * A new kind of `Event`s should be created: `TerminationEvent` which include `Completed`, `Failed`, and `Canceled`
 * `TerminationEvent`s will be broadcasted on an exchange which will be bound to all interested components later
 * `EventSourcingSystem.dipatch` should use `RabbitMQ` to dispatch `Event`s instead of triggering local `Listener`s
 * Any node can be notified when a `Task` emits a termination event

