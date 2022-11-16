# Real-Time Enrichment Application Workshop
## Getting Started

Learning Objectives:
1. quickly deploy infrastructure components and enable metrics
2. Configure an OTEL Collector to collect metrics from various sources
3. instrument Python apps for tracing
4. instrument Java apps for tracing and code profiling
5. Have all items above running in Kubernetes
6. View the data in the Splunk O11y UI
7. Build dashboards to monitor this application

Note: Since this app consists of four flask apps and two java apps, this main workshop document will provide step-by-step commands to deploy and instrument one flask app and one java app. The process and commands follow a similar pattern for their respective languages and step-by-step instructions for the remaining apps are available in a separate doc.

Prerequisites
1. Yelp dataset placed in /var/appdata/
2. Splunk Observability Cloud access key
3. Clone this repository
4. a K8 environment (e.g. k3s on multipass)
5. python3
6. Docker Desktop and a dockerhub account

```
#1. Yelp dataset placed in /var/appdata/
#extract then move the yelp dataset (json) to /var/appdata
sudo mkdir -p /var/appdata/
sudo chmod 777 /var/appdata/
mv <yelp*json> /var/appdata/
stevel@C02G312EMD6R ~ % ll /var/appdata/yelp*
-rw-r--r--@ 1 stevel  staff   124380583 Jan 28  2021 /var/appdata/yelp_academic_dataset_business.json
-rw-r--r--@ 1 stevel  staff  6936678061 Jan 28  2021 /var/appdata/yelp_academic_dataset_review.json
-rw-r--r--@ 1 stevel  staff  3684505303 Jan 28  2021 /var/appdata/yelp_academic_dataset_user.json

#3. Clone this repo
cd  /var/appdata
git clone https://github.com/leungsteve/realtime_enrichment.git

#4. a K8 environment (e.g. k3s on multipass)
#create a multipass VM., present the yelp dataset to the VM
multipass launch --name test4cpu8gb --cpus 4 --mem 8Gb --disk 32GB
#makes the yelp dataset available to your VM
multipass mount /var/appdata test4cpu8gb
multipass shell test4cpu8gb
ubuntu@test4cpu8gb:~$ ll /var/appdata/yelp*
-rw-r--r-- 1 ubuntu ubuntu  124380583 Jan 28  2021 /var/appdata/yelp_academic_dataset_business.json
-rw-r--r-- 1 ubuntu ubuntu 6936678061 Jan 28  2021 /var/appdata/yelp_academic_dataset_review.json
-rw-r--r-- 1 ubuntu ubuntu 3684505303 Jan 28  2021 /var/appdata/yelp_academic_dataset_user.json

#5. python3
#recommended to create and work in a virual environment (on your mac. not in multipass)
python3 --version
Python 3.10.0

cd Workshop/flask_apps_start
python3 -m venv rtapp-workshop
source rtapp-workshop/bin/activate

#6. Docker Desktop and a dockerhub account
#login to your dockerhub account and create an access token at https://hub.docker.com/settings/security
#Note that this information will not be dispalyed again. Take necessary precaution to keep this secured.
```
## Deploy infrastructure components and capture metrics
We often need to work with different applications and services, some of which we may not be familiar with. Fortunately helm charts can be leveraged to spin up complex applications or services that are fully functional.

How to find helm charts?

This demo application leverages Kafka and MongoDB. 
* [Apache Kafka packaged by Bitnami](https://github.com/bitnami/charts/tree/master/bitnami/spark/#installing-the-chart)
  * Review the page, especially the different options. You can toggle useful features on or create a sizable Kafka cluster. 
  * Configure the following options:
    * --set replicaCount=3
    * --set metrics.jmx.enabled=true
    * --set metrics.kafka.enabled=true
    * --set zookeeper.metrics.enabled=true 
    * --set deleteTopicEnable=true
* [MongoDB(R) packaged by Bitnami](`https://github.com/bitnami/charts/tree/master/bitnami/mongodb/#installing-the-chart`)
  * Review the page, especially the different options.
  * Configure the following options:
    * --set metrics.enabled=true
    * --set global.namespaceOverride=default
    * --set auth.rootUser=root
    * --set auth.rootPassword=splunk 
    * --set auth.enabled=false
### Install Kafka and MongoDB with helm charts
```
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install kafka --set replicaCount=3 --set metrics.jmx.enabled=true --set metrics.kafka.enabled=true  --set zookeeper.metrics.enabled=true --set deleteTopicEnable=true bitnami/kafka
helm install mongodb --set metrics.enabled=true bitnami/mongodb --set global.namespaceOverride=default --set auth.rootUser=root --set auth.rootPassword=splunk --set auth.enabled=false
```

### Install the Splunk OTEL helm chart.
Using information for each Helm chart and Splunk O11y Data Setup, generate a values.yaml for the different infrastructure components (Kafka, Zookeeper, MongoDB). values.yaml for the different services will be passed to the helm install command.

We will use Kafka as an example here (values.yaml for other services provided):

References:
* [Apache Kafka packaged by Bitnami](https://github.com/bitnami/charts/tree/master/bitnami/spark/#installing-the-chart)
* [Configure application receivers for databases » Apache Kafka](https://docs.splunk.com/Observability/gdi/kafka/kafka.html)
* [kafkametricsreceiver](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/kafkametricsreceiver)

```
otelAgent:
  config:
    receivers:
      receiver_creator:
        receivers:
          smartagent/kafka:
            rule: type == "pod" && name matches "kafka"
            config:
                    #endpoint: '`endpoint`:5555'
              port: 5555
              type: collectd/kafka
              clusterName: sl-kafka
otelK8sClusterReceiver:
  k8sEventsEnabled: true
  config:
    receivers:
      kafkametrics:
        brokers: kafka:9092
        protocol_version: 2.0.0
        scrapers:
          - brokers
          - topics
          - consumers
    service:
      pipelines:
        metrics:
          receivers:
                  #- prometheus
          - k8s_cluster
          - kafkametrics
```
Deploy the OTEL collector.
```
export SPLUNK_ACCESS_TOKEN=<your access token>
export SPLUNK_REALM=<your realm>

#use your own cluster name
export clusterName=sl-K3s
helm repo add splunk-otel-collector-chart https://signalfx.github.io/splunk-otel-collector-chart
helm repo update
helm install --set provider=' ' --set distro=' ' --set splunkObservability.accessToken=$SPLUNK_ACCESS_TOKEN --set clusterName=$clusterName --set splunkObservability.realm=$SPLUNK_REALM --set otelCollector.enabled='false' --set splunkObservability.logsEnabled='true' --set gateway.enabled='false' --values kafka.values.yaml --values mongodb.values.yaml --values zookeeper.values.yaml --values alwayson.values.yaml --values k3slogs.yaml --generate-name splunk-otel-collector-chart/splunk-otel-collector 
```
Verify that the Kafka, MongoDB and Splunk OTEL Collector helm charts are installed. Note that names may differ.
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

## From code to running apps in K8 (no Splunk yet)
Objective: understand activities to run code in K8. 
* start with code
* containerize the app
* deploy the app in Kubernetes

### Start with code - Inspect review.py and requirements.txt
Install the necessary python packages
```
pip freeze
pip install -r requirements.txt
pip freeze 
```

Start the reviews service
```
python review.py
```
output: 
```
(venv) stevel@C02G312EMD6R flask_apps_start % python review.py 
 * Serving Flask app 'review' (lazy loading)
 * Environment: production
         ...snip...
 * Running on http://10.160.145.246:5000/ (Press CTRL+C to quit)
 * Restarting with stat
127.0.0.1 - - [17/May/2022 22:46:38] "GET / HTTP/1.1" 200 -
127.0.0.1 - - [17/May/2022 22:47:02] "GET /get_review HTTP/1.1" 200 -
127.0.0.1 - - [17/May/2022 22:47:58] "GET /get_review HTTP/1.1" 200 -
```
Test the reviews service with a browser and access the following URLS:
1. http://localhost:5000
2. http://localhost:5000/get_review

Alternatively, test with curl
```
curl localhost:5000
{
  "message": "Hello, you want to hit /get_review. We have 100000 reviews!"
}

curl localhost:30000/get_review
{"review_id":"NjbiESXotcEdsyTc4EM3fg","user_id":"PR9LAM19rCM_HQiEm5OP5w","business_id":"UAtX7xmIfdd1W2Pebf6NWg","stars":3.0,"useful":0,"funny":0,"cool":0,"text":"-If you're into cheap beer (pitcher of bud-light for $7) decent wings and a good time, this is the place for you. Its generally very packed after work hours and weekends. Don't expect cocktails. \n\n-You run into a lot of sketchy characters here sometimes but for the most part if you're chilling with friends its not that bad. \n\n-Friendly bouncer and bartenders.","date":"2016-04-12 20:23:24"}
```

Note: You can kill the app with control+C

Questions:
1. What does this application do?
2. What port will this service listen on when it is running?
3. Is this different from the other Flask apps? Does it matter?
4. Do you see the yelp dataset being used?
5. Compare the output of pip freeze. Why did the list change?

### Containerize the review app
To containerize the review service, we need to:
1. create a Dockerfile
2. create a container image (locally)
3. push the container image into a container repository

#### Create a Dockerfile for review
1. identify an appropriate image
   1. ubuntu vs. python vs. alpine/slim
      1. ubuntu - overkill, large image size, wasted resources when running in K8
      2. this is a python app, so pick an image that is optimized for it
      3. [avoid alpine for python](https://lih-verma.medium.com/alpine-makes-python-docker-builds-way-too-50-slower-and-images-double-2-larger-61d1d43cbc79)
2. order matters 
   1. you're building layers.
   2. re-use the layers as much as possible
   3. have items that change often towards the end
3. Other [Best practices for writing Dockerfiles](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/) 
```
FROM python:3.10-slim
WORKDIR /app
COPY requirements.txt /app
RUN pip install -r requirements.txt
COPY ./review.py /app
EXPOSE 5000
CMD [ "python", "review.py" ]
```

#### Create a container image for review

Note: Substitute your own repo and use an appropriate version number. Increment the version as you make changes and roll new images and recommend avoid re-using version numbers since K8 will cache images.
```
(rtapp-workshop) stevel@C02G312EMD6R review % docker build -t localhost:8000/review:0.1 .
[+] Building 35.5s (11/11) FINISHED
 => [internal] load build definition from Dockerfile                              0.0s
         ...snip...
 => [3/5] COPY requirements.txt /app                                              0.0s
 => [4/5] RUN pip install -r requirements.txt                                     4.6s
 => [5/5] COPY ./review.py /app                                                   0.0s
 => exporting to image                                                            0.2s
 => => exporting layers                                                           0.2s
 => => writing image sha256:61da27081372723363d0425e0ceb34bbad6e483e698c6fe439c5  0.0s
 => => naming to docker.io/localhost:8000/review:0.1                                   0.0
```


#### Push the review container image into a container repository

Note: If needed, logon to dockerhub
```
docker login -u localhost:8000
Password:
Login Succeeded
```
```
docker push localhost:8000/review:0.1
The push refers to repository [docker.io/localhost:8000/review-splkotel]
02c36dfb4867: Pushed
         ...snip...
fd95118eade9: Pushed
0.1: digest: sha256:3651f740abe5635af95d07acd6bcf814e4d025fcc1d9e4af9dee023a9b286f38 size: 2202
```

Verify that your image is in dockerhub. The same info can be found in Docker Desktop
![img.png](../images/dockerhub.png)


### Deploy the review app in Kubernetes
If needed, create a secret so K8 can authenticate with your repo on github.

Reference: [Pull an Image from a Private Registry](https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/)
```
sudo docker login
Login with your Docker ID to push and pull images from Docker Hub. If you don't have a Docker ID, head over to https://hub.docker.com to create one.
Username: localhost:8000
Password:
WARNING! Your password will be stored unencrypted in /home/ubuntu/.docker/config.json
Login Succeeded

#this makes the config file with your docker credentials readable by yourself (and not root)
sudo chown `whoami`:`whoami` ~/.docker/config.json

#this creates a k8 secret which will be used by your deployments to authenticate against docker hub when it pulls container images
sudo kubectl create secret generic regcred --from-file=.dockerconfigjson=/home/ubuntu/.docker/config.json --type=kubernetes.io/dockerconfigjson
```

#### Create K8 deployment and service yaml files for the review app
Reference: [Creating a Deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#creating-a-deployment)
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: review
  labels:
    app: review
spec:
  replicas: 1
  selector:
    matchLabels:
      app: review
  template:
    metadata:
      labels:
        app: review
    spec:
      imagePullSecrets:
      - name: regcred
      containers:
      - image: review/review:0.1
        name: review
        volumeMounts:
        - mountPath: /var/appdata
          name: appdata
      volumes:
      - name: appdata
        hostPath:
          path: /var/appdata
```
Notes:
1. labels - K8 uses labels and selectors to tag and identify resources
   1. For example, in the next step, we'll create a service and associate it to this deployment using the label
2. replicas = 1
   1. K8 allows you to scale your deployments horizontally
   2. We'll leverage this later to add load and increase our ingestion rate 
3. regcred provide this deployment with the ability to access your dockerhub credentials which is necessary to pull the container image.
4. The volume definition and volumemount make the yelp dataset visible to the container

Reference: [Creating a service](https://kubernetes.io/docs/concepts/services-networking/service/#defining-a-service)
```
apiVersion: v1
kind: Service
metadata:
  name: review
spec:
  type: NodePort
  selector:
    app: review
  ports:
    - port: 5000
      targetPort: 5000
      nodePort: 30000
```
Notes about review.service.yaml:
1. the selector associates this service to pods with the label app with the value being review
2. the review service exposes the review pods as a network service
   1. other pods can now ping 'review' and they will hit a review pod.
   2. a pod would get a review if it ran 'curl http://review:5000'
3. NodePort service
   1. the service is accessible to the K8 host by the nodePort, 30000
   2. Another machine that has this can get a review if it ran 'curl http://<k8 host ip>:30000'

#### Apply the review deployment and service
```
kubectl apply -f review.service.yaml -f review.deployment.yaml
```

Verify that the deployment and services are running:
```
ubuntu@ip-10-0-1-54:/tmp$ kubectl get deployments
NAME                                                    READY   UP-TO-DATE   AVAILABLE   AGE
review                                                  1/1     1            1           19h

ubuntu@ip-10-0-1-54:/tmp$ kubectl get services
NAME                       TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                         AGE
review                     NodePort    10.43.175.21    <none>        5000:30000/TCP                  154d

ubuntu@ip-10-0-1-54:/tmp$ curl localhost:30000
{
  "message": "Hello, you want to hit /get_review. We have 100000 reviews!"
}
ubuntu@ip-10-0-1-54:/tmp$ curl localhost:30000/get_review
{"review_id":"Vv9rHtfBrFc-1M1DHRKN9Q","user_id":"EaNqIwKkM7p1bkraKotqrg","business_id":"TA1KUSCu8GkWP9w0rmElxw","stars":3.0,"useful":1,"funny":0,"cool":0,"text":"This is the first time I've actually written a review for Flip, but I've probably been here about 10 times.  \n\nThis used to be where I would take out of town guests who wanted a good, casual, and relatively inexpensive meal.  \n\nI hadn't been for a while, so after a long day in midtown, we decided to head to Flip.  \n\nWe had the fried pickles, onion rings, the gyro burger, their special burger, and split a nutella milkshake.  I have tasted all of the items we ordered previously (with the exception of the special) and have been blown away with how good they were.  My guy had the special which was definitely good, so no complaints there.  The onion rings and the fried pickles were greasier than expected.  Though I've thought they were delicious in the past, I probably wouldn't order either again.  The gyro burger was good, but I could have used a little more sauce.  It almost tasted like all of the ingredients didn't entirely fit together.  Something was definitely off. It was a friday night and they weren't insanely busy, so I'm not sure I would attribute it to the staff not being on their A game...\n\nDon't get me wrong.  Flip is still good.  The wait staff is still amazingly good looking.  They still make delicious milk shakes.  It's just not as amazing as it once was, which really is a little sad.","date":"2010-10-11 18:18:35"}
```


### Start with code - Inspect ReviewsProducer.java and pom.xml
This application performs the following:
* makes a Kafka connection
* repeatedly GETs a review from the review service and places it in the review kafka topic


Note: the reviewsProducer.jar is provided. Building this is out of scope.

### Containerize the ReviewsProducer app
To containerize the ReviewsProducer, we need to:
1. create a Dockerfile
2. create a container image
3. push the container image into a container repository

#### Create a Dockerfile for ReviewsProducer
```
FROM openjdk:11
COPY ReviewsProducer.jar /ReviewsProducer.jar
CMD ["java", "ReviewsProducer.jar"]
```
#### Create a container image for ReviewsProducer
Note: the repository name must be lowercase
```
(rtapp-workshop) stevel@C02G312EMD6R java % docker build -f Dockerfile.ReviewsProducer -t localhost:8000/reviewsproducer:0.01 .
[+] Building 40.1s (7/7) FINISHED
         ...snip...                  0.1s
 => => writing image sha256:718c14255f1f492e488da98ee3e1a4f2e1dfb7f8f2fbdc438b6ab57776d0396b                 0.0s
 => => naming to docker.io/localhost:8000/reviewsproducer:0.01                                                    0.0s
```
#### Push the ReviewsProducer container image into a container repository
```
docker push localhost:8000/reviewsproducer:0.01
```












#### Apply the instructions from Data Setup
Within the O11y Cloud UI: 

Data Setup -> Monitor Applications -> Python (traces) -> Add Integration

Provide the following:
* Service: review 
* Django: no 
* collector endpoint: http://localhost:4317
* Environment: demo
* Kubernets: yes 
* Legacy Agent: no

Follow the instructions to _Install the instrumentation packages for your Python environment._
```
(rtapp-workshop) stevel@C02G312EMD6R flask_apps_start % pip install splunk-opentelemetry
Collecting splunk-opentelemetry
     ...snip...
Collecting typing-extensions>=3.7.4
  Downloading typing_extensions-4.2.0-py3-none-any.whl (24 kB)
WARNING: You are using pip version 21.2.3; however, version 22.1 is available.
You should consider upgrading via the '/Users/stevel/Documents/Workshop/rtapp-workshop/bin/python3 -m pip install --upgrade pip' command.

(rtapp-workshop) stevel@C02G312EMD6R flask_apps_start % pip install opentelemetry-exporter-otlp-proto-grpc
Collecting opentelemetry-exporter-otlp-proto-grpc
  Downloading opentelemetry_exporter_otlp_proto_grpc-1.11.1-py3-none-any.whl (18 kB)
Collecting grpcio<2.0.0,>=1.0.0
  Downloading grpcio-1.46.1-cp310-cp310-macosx_10_10_universal2.whl (4.2 MB)
     |████████████████████████████████| 4.2 MB 646 kB/s
    ...snip...
equirement already satisfied: typing-extensions>=3.7.4 in /Users/stevel/Documents/Workshop/rtapp-workshop/lib/python3.10/site-packages (from opentelemetry-sdk~=1.11->opentelemetry-exporter-otlp-proto-grpc) (4.2.0)
Requirement already satisfied: opentelemetry-semantic-conventions==0.30b1 in /Users/stevel/Documents/Workshop/rtapp-workshop/lib/python3.10/site-packages (from opentelemetry-sdk~=1.11->opentelemetry-exporter-otlp-proto-grpc) (0.30b1)
Installing collected packages: six, protobuf, opentelemetry-proto, grpcio, googleapis-common-protos, backoff, opentelemetry-exporter-otlp-proto-grpc
Successfully installed backoff-1.11.1 googleapis-common-protos-1.56.1 grpcio-1.46.1 opentelemetry-exporter-otlp-proto-grpc-1.11.1 opentelemetry-proto-1.11.1 protobuf-3.20.1 six-1.16.0
WARNING: You are using pip version 21.2.3; however, version 22.1 is available.
You should consider upgrading via the '/Users/stevel/Documents/Workshop/rtapp-workshop/bin/python3 -m pip install --upgrade pip' command.

(rtapp-workshop) stevel@C02G312EMD6R flask_apps_start % splunk-py-trace-bootstrap
Collecting opentelemetry-instrumentation-aws-lambda==0.30b1
  Downloading opentelemetry_instrumentation_aws_lambda-0.30b1-py3-none-any.whl (11 kB)
Requirement already satisfied: opentelemetry-instrumentation==0.30b1 in /Users/stevel/Documents/Workshop/rtapp-workshop/lib/python3.10/site-packages (from opentelemetry-instrumentation-aws-lambda==0.30b1) (0.30b1)
     ...snip...
Requirement already satisfied: Deprecated>=1.2.6 in /Users/stevel/Documents/Workshop/rtapp-workshop/lib/python3.10/site-packages (from opentelemetry-api~=1.3->opentelemetry-instrumentation-jinja2==0.30b1) (1.2.13)
Installing collected packages: opentelemetry-instrumentation-jinja2
Successfully installed opentelemetry-instrumentation-jinja2-0.30b1
WARNING: You are using pip version 21.2.3; however, version 22.1 is available.
You should consider upgrading via the '/Users/stevel/Documents/Workshop/rtapp-workshop/bin/python3 -m pip install --upgrade pip' command.
```


