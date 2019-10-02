# 5. Distributed Task termination ackowledgement

Date: 2019-10-02

## Status

Accepted

## Context

By switching the task manager to a distributed implementation, we need to be able to execute a `Task` on any node of the cluster.
We need to have a way to broadcast some `Event`s (`CancelRequested` and termination `Event`s).

## Decision

 * Creating a `RabbitMQEventHandler` which publish `Event`s to RabbitMQ
 * the `Completed`, `Failed`, `Canceled` events will be broadcasted on an exchange


## Consequences

 * `EventSourcingSystem.dipatch` should use `RabbitMQ` to dispatch `Event`s instead of triggering local `Listener`s
 * Any node can be notified when a `Task` emits a termination event

