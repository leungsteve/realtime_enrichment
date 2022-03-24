---
title: "Overview"
linkTitle: "Overview"
date: 2022-03-18T09:12:37-05:00
draft: false
weight: 1
---

This is a real-time demo application that has been auto-instrumented for Splunk Observability Cloud. Instructions and configuration files are also provided to set up K3s, a Kafka cluster and MongoDB running on a Mac (intel). When completed, logs, metrics and traces will be visible in Splunk Observability Cloud for this application and the underlying infrastructure components.

## System Architecture

At a high-level, the application consists of the following microservices:
### Flask Apps
1) `review` - returns a random yelp review
2) `sentiment` - returns a sentiment score
3) `business_lookup` - returns business details for a given business_id
4) `user_lookup` - returns user details for a given user_id

### Java Apps
1) `ReviewsProducer` - obtains a random review from the review microservice and publishes it to the reviews Kafka topic
2) `ReviewsConsumerToMongo` - consumes messages from the reviews Kafka topic, enriches each message with user details, business details and a sentiment score then inserts this as a single document into MongoDB

The following illustrates how the different microservices interact with one another.

![Application Data Pipeline](../images/Architecture.jpg)

## Environment
The following illustrates the environment where this application is running on and how the logs, metrics and traces are captured and sent to Splunk Observability Cloud.

![infrastructure](../images/Infrastructure.jpg)

