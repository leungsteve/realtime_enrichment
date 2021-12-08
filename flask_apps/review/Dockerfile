FROM python:3.10-slim

WORKDIR /app
COPY requirements.txt /app
RUN pip install -r requirements.txt
RUN splk-py-trace-bootstrap

COPY ./review.py /app

EXPOSE 5000
ENTRYPOINT [ "splunk-py-trace" ]
CMD [ "python", "review.py" ]

#docker build -t leungsteve/review-splkotel:1.0 . 2> /tmp/dockerbuild_log.txt
#docker push leungsteve/review-splkotel:1.0