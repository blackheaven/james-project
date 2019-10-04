# 3. Distributed WorkQueue

Date: 2019-10-02

## Status

Accepted (lazy consensus)

## Context

By switching the task manager to a distributed implementation, we need to be able to run a `Task` on any node of the cluster.

## Decision
  For the time being we will keep the sequential execution property of the task manager.
  This is an intermediate milestone torward the final implementation which will drop this property.

 * Use a queue rabbitmq as a workqueue where only the `Created` events are pushed into.
   This queue will be exclusive and with a `prefetch = 1`.
   The queue will listen to the worker on the same node and will ack the message only once it is finished (`Completed`, `Failed`, `Cancelled`)

## Consequences

 * It's a temporary and not safe to use in production solution, if the node promoted to exclusive listener of the queue die, no more tasks would be run
 * the sequential execution impose this slow execution model.
 * Works should be done to replace the sequential execution property of the task manager by a set of constraints on the `Task`.

