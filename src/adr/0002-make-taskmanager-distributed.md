# 2. Make TaskManager Distributed

Date: 2019-10-02

## Status

Accepted (lazy consensus)

## Context

In order to have a distributed version of James we need to have an unique way to deal with `Task`.

Currently `TaskManager` is independent and node-exclusive.
We are not allowed to get all the `Task`s of a cluster.

## Decision

Create a distribution-aware implementation of `TaskManager`.

## Consequences

 * Split the `TaskManager` part dealing with the coordination (`Task` management and view) and the `Task` execution (located in `TaskManagerWorker`)
 * The distributed `TaskManager` will rely on RabbitMQ to coordinate and the `EventSystem` to synchronize states
