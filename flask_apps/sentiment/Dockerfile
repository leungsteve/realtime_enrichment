FROM python:3.10-slim

WORKDIR /app
COPY requirements.txt /app
RUN pip install -r requirements.txt
RUN splk-py-trace-bootstrap

COPY ./sentiment.py /app

EXPOSE 5001
ENTRYPOINT [ "splunk-py-trace" ]
CMD [ "python", "sentiment.py" ]

#docker build -t leungsteve/sentiment-splkotel:1.0 . 2> ./dockerbuild_log.txt
#docker push leungsteve/sentiment-splkotel:1.0