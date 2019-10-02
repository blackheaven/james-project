# 6. Task serialization

Date: 2019-10-02

## Status

Accepted

## Context

By switching the task manager to a distributed implementation, we need to be able to execute a `Task` on any node of the cluster.
We need to have a way to describe the `Task` to be executed and serialize it in order to be able to store it in the `Created` event. Which will be persisted in the Event Store, and will be send in the eventbus.

At this point in time a `Task` can contains any arbitrary code. It's not an element of a finite set of actions.

## Decision

 * Create a `Factory` for one `Task`
 * Inject a `Factory` `Registry` via a Guice Module
 * The `Task` `Serialization` will be done in json, We will get inspired by `EventSerializer`.

## Consequences

 * Every `Task` should be serializable.
 * Every `Task` should provide a `Factory` which would be responsible to deserialize the task and instantiate it.
 * Every `Factory` should be registered through a Guice module to be created for each project containing a `Factory`
