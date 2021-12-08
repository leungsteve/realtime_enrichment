#Real-time demo app

##Introduction

This is a real-time demo application that has been auto-instrumented for Splunk Observability Cloud. Instructions and configuration files are also provided to set up K3s, a Kafka cluster and MongoDB running on a Mac (intel). When completed, logs, metrics and traces will be visible in Splunk Observability Cloud for this application and the underlying infrastructure components.

At a high-level, the application consists of the following microservices:
- Flask Apps:
1) review - returns a random yelp review
2) sentiment - returns a sentiment score
3) business_lookup - returns business details for a given business_id
4) user_lookup - returns user details for a given user_id

- Java Apps
1) ReviewsProducer - obtains a random review from the review microservice and publishes it to the reviews Kafka topic
2) ReviewsConsumerToMongo - consumes messages from the reviews Kafka topic, enriches each message with user details, business details and a sentiment score then inserts this as a single document into MongoDB

The following illustrates how the different microservices interact with one another.

![Application Data Pipeline](./images/Architecture.jpg)

The following illustrates the environment where this application is running on and how the logs, metrics and traces are captured and sent to Splunk Observability Cloud.

![infrastructure](./images/Infrastructure.jpg)

##Prerequisites

- Splunk Observability Cloud Access Key
- clone this repository
- Download and extract the Yelp Data Set to /var/appdata such that the following files are available:
```
ll /var/appdata/yelp_academic_dataset_*
-rw-r--r--@ 1 stevel  staff   124380583 Jan 28  2021 /var/appdata/yelp_academic_dataset_business.json
-rw-r--r--@ 1 stevel  staff  6936678061 Jan 28  2021 /var/appdata/yelp_academic_dataset_review.json
-rw-r--r--@ 1 stevel  staff  3684505303 Jan 28  2021 /var/appdata/yelp_academic_dataset_user.json
```


##Create and configure a Multipass VM with K3s and helm

1) Create a multipass VM. We will use test4cpu8gb as my VM name throughout this example.
```
multipass launch --name test4cpu8gb --cpus 4 --mem 8Gb --disk 32GB
```
2) Mount /var/appdata to the VM to make the yelp dataset files available to the VM.
```
multipass mount /var/appdata test4cpu8gb
```
3) shell into the vm and verify that /var/appdata is mounted.
```
multipass shell test4cpu8gb

ubuntu@test4cpu8gb:~$ ll /var/appdata/yelp*
-rw-r--r-- 1 ubuntu ubuntu  124380583 Jan 28  2021 /var/appdata/yelp_academic_dataset_business.json
-rw-r--r-- 1 ubuntu ubuntu 6936678061 Jan 28  2021 /var/appdata/yelp_academic_dataset_review.json
-rw-r--r-- 1 ubuntu ubuntu 3684505303 Jan 28  2021 /var/appdata/yelp_academic_dataset_user.json
```
4) Install K3s and helm. Run the following commands from the VM.
```
curl -sfL https://get.k3s.io | sh -
curl -s https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash
sudo mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml .kube/config
```

## Install and get metrics for K8, Kafka and MongoDB
1) Install the Kafka and MongoDB with helm charts
```
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install kafka --set replicaCount=3 --set metrics.jmx.enabled=true --set metrics.kafka.enabled=true  --set zookeeper.metrics.enabled=true --set deleteTopicEnable=true bitnami/kafka
helm install mongodb --set metrics.enabled=true bitnami/mongodb --set global.namespaceOverride=default --set auth.rootUser=root --set auth.rootPassword=splunk --set auth.enabled=false
```
2) Install the Splunk OTEL helm chart. In this example, the K8 cluster name is sl-k8. Notice that values.yaml files are provided for Zookeeper, MongoDB and Kafka so metrics for these components will be captured. 
```
helm install --set provider=' ' --set distro=' ' --set splunkObservability.accessToken=$ACCESS_TOKEN --set clusterName='sl-k8' --set splunkObservability.realm='us0' --set otelCollector.enabled='false'  --values kafka.values.yaml --values mongodb.values.yaml --values zookeeper.values.yaml --generate-name splunk-otel-collector-chart/splunk-otel-collector 
```

3) Verify that the Kafka, MongoDB and Splunk Otel Collector helm charts are installed. Note that names may differ.
```
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
![K8Navigator](./images/K8Navigator.jpg)

The Kafka Brokers Dashboard shows metrics for our Kafka Cluster:
![Kafka](./images/kafka_brokers.jpg)

The MongoDB Host Dashboard shows metrics for MongoDB:
![MongoDB](./images/mongodb_connections.jpg)

##Deploy the Real-time Demo Application
YAML files for K8 deployment and services are provided in the k8_yamls folder. To deploy the application, simply apply all of these files.
```
cd k8_yamls
kubectl apply -f .
```

Note that the replicas for the reviewsproducer is set to 0.
```
ubuntu@test4cpu8gb:/var/appdata/k8_yamls$ kubectl get deployments.apps
NAME                                                    READY   UP-TO-DATE   AVAILABLE   AGE
userlookup                                              1/1     1            1           158m
review                                                  1/1     1            1           161m
sentiment                                               1/1     1            1           159m
mongodb                                                 1/1     1            1           106m
businesslookup                                          1/1     1            1           158m
kafka-exporter                                          1/1     1            1           3h58m
splunk-otel-collector-1638910184-k8s-cluster-receiver   1/1     1            1           3h57m
reviewsconsumer                                         1/1     1            1           63m
mongodbdetails                                          1/1     1            1           56m
reviewsproducer                                         0/0     0            0           79m
```
When you're ready, you can scale this deployment to 1 to begin the pipeline.
```
kubectl scale deployment reviewsproducer --replicas=1
```

You will see the following in the APM service map for this application.

![apmservicemap](./images/servicemapworking.png)


