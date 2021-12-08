FROM python:3.10-slim

WORKDIR /app
COPY requirements.txt /app
RUN pip install -r requirements.txt
RUN splk-py-trace-bootstrap

COPY ./user_lookup.py /app

EXPOSE 5003
ENTRYPOINT [ "splunk-py-trace" ]
CMD [ "python", "user_lookup.py" ]

#docker build -t leungsteve/userlookup-splkotel:1.0 . 2> ./dockerbuild_log.txt
#docker push leungsteve/userlookup-splkotel:1.0