FROM python:3.10-slim

WORKDIR /app
COPY requirements.txt /app
RUN pip install -r requirements.txt
RUN splk-py-trace-bootstrap

COPY ./mongodb_details.py /app

EXPOSE 6000
ENTRYPOINT [ "splunk-py-trace" ]
CMD [ "python", "mongodb_details.py" ]

#docker build -t leungsteve/mongodbdetails-splkotel:1.0 . 2> ./dockerbuild_log.txt
#docker push leungsteve/mongodbdetails-splkotel:1.0
