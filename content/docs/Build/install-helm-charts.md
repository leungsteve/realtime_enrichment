---
title: "Install Helm Charts"
linkTitle: "Install Helm Charts"
weight: 10
draft: false
---

## Install Helm Charts and get metrics for K8, Kafka and MongoDB

Install the Kafka and MongoDB with helm charts. Note that for Kafka, replicaCount=3. This provides uw with 3 brokers. We're also enabling metrics for Kafka and Zookeeper. Finally, we're allowing topics to be deleted since this is a demo environment. Note that MongoDB is also configured with metrics enabled and a weak username and password since this is a demo environment.
### Add Helm Charts for Kafka and MongoDB
Install the Helm Charts for Kafka and MongoDB.  This will automatically provide MongoDB and the required Kafka resource to our Application stack
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install kafka --set replicaCount=3 --set metrics.jmx.enabled=true --set metrics.kafka.enabled=true  --set zookeeper.metrics.enabled=true --set deleteTopicEnable=true bitnami/kafka
helm install mongodb --set metrics.enabled=true bitnami/mongodb --set global.namespaceOverride=default --set auth.rootUser=root --set auth.rootPassword=splunk --set auth.enabled=false
```

### Install Splunk Otel Helm Chart

Install the Splunk OTEL helm chart. In this example, the K8 cluster name is `sl-K3s`. Notice that `values.yaml` files are provided for Zookeeper, MongoDB and Kafka so metrics for these components will be captured.
```bash
helm repo add splunk-otel-collector-chart https://signalfx.github.io/splunk-otel-collector-chart
helm repo update
helm install --set provider=' ' --set distro=' ' --set splunkObservability.accessToken=$SPLUNK_ACCESS_TOKEN --set clusterName='sl-K3s' --set splunkObservability.realm=$SPLUNK_REALM --set otelCollector.enabled='false' --set splunkObservability.logsEnabled='true' --set gateway.enabled='false' --values kafka.values.yaml --values mongodb.values.yaml --values zookeeper.values.yaml --values alwayson.values.yaml --values k3slogs.yaml --generate-name splunk-otel-collector-chart/splunk-otel-collector 
```

### Verify Helm Chart Installation

Verify that the Kafka, MongoDB and Splunk Otel Collector helm charts are installed. Note that names may differ.

```bash
ubuntu@test4cpu8gb:~/otel$ helm list
NAME                            	NAMESPACE	REVISION	UPDATED                                	STATUS  	CHART                       	APP VERSION
kafka                           	default  	1       	2021-12-07 12:48:47.066421971 -0800 PST	deployed	kafka-14.4.1                	2.8.1
mongodb                         	default  	1       	2021-12-07 12:49:06.132771625 -0800 PST	deployed	mongodb-10.29.2             	4.4.10
splunk-otel-collector-1638910184	default  	1       	2021-12-07 12:49:45.694013749 -0800 PST	deployed	splunk-otel-collector-0.37.1	0.37.1

ubuntu@test4cpu8gb:~/otel$ kubectl get pods
NAME                                                              READY   STATUS    RESTARTS   AGE
kafka-zookeeper-0                                                 1/1     Running   0          18m
kafka-2                                                           2/2     Running   1          18m
mongodb-79cf87987f-gsms8                                          2/2     Running   0          18m
kafka-1                                                           2/2     Running   1          18m
kafka-exporter-7c65fcd646-dvmtv                                   1/1     Running   3          18m
kafka-0                                                           2/2     Running   1          18m
splunk-otel-collector-1638910184-agent-27s5c                      2/2     Running   0          17m
splunk-otel-collector-1638910184-k8s-cluster-receiver-8587qmh9l   1/1     Running   0          17m
```

Details about this K3s node is available in the K8 Navigator. Note that Related Context Link to Log Observer for this K8 node at the bottom.
![K8Navigator](../images/K8Navigator.jpg)

The Kafka Brokers Dashboard shows metrics for our Kafka Cluster:
![Kafka](./images/kafka_brokers.jpg)

The MongoDB Host Dashboard shows metrics for MongoDB:
![MongoDB](./images/mongodb_connections.jpg)