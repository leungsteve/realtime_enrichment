---
title: "Troubleshooting and Cleanup"
linkTitle: "Troubleshooting and Cleanup"
weight: 40
---

## Troubleshooting and cleanup notes:
If you need to run kafka console commands start the kafka-client pod:
```
kubectl run kafka-client --restart='Never' --image docker.io/bitnami/kafka:2.8.1-debian-10-r73 --namespace default --command -- sleep infinity
```
You can exec (shell) into the kafka client pod or run a command
```
kubectl exec --tty -i kafka-client --namespace default -- bash

or

kubectl exec kafka-client -- kafka-topics.sh --bootstrap-server kafka.default.svc.cluster.local:9092 --list
kubectl exec kafka-client -- kafka-topics.sh --bootstrap-server kafka.default.svc.cluster.local:9092 --describe --topic reviews
kubectl exec kafka-client -- kafka-topics.sh --bootstrap-server kafka.default.svc.cluster.local:9092 --delete --topic reviews
kubectl exec kafka-client -- kafka-topics.sh --bootstrap-server kafka.default.svc.cluster.local:9092 --create --topic reviews --replication-factor 2 --partitions 5

```

To connect to MongoDB and cleanup (delete the O11yCollection):
```
kubectl run --namespace default mongodb-client --rm --tty -i --restart='Never' --env="MONGODB_ROOT_PASSWORD=$MONGODB_ROOT_PASSWORD" --image docker.io/bitnami/mongodb:4.4.10-debian-10-r44 --command -- bash
mongo admin --host "mongodb"
use O11y
db.O11yCollection.drop()
exit
exit
```

To run curl:
```
kubectl run curl --restart='Never' --image curlimages/curl --command -- sleep infinity
kubectl exec curl -- curl -s http://www.google.com