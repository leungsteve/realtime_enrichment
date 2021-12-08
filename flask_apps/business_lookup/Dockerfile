FROM python:3.10-slim

WORKDIR /app
COPY requirements.txt /app
RUN pip install -r requirements.txt
RUN splk-py-trace-bootstrap

COPY ./business_lookup.py /app

EXPOSE 5002
ENTRYPOINT [ "splunk-py-trace" ]
CMD [ "python", "business_lookup.py" ]

#docker build -t leungsteve/businesslookup-splkotel:1.0 . 2> ./dockerbuild_log.txt
#docker push leungsteve/businesslookup-splkotel:1.0