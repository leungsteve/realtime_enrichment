pip install 'splunk-opentelemetry[all]'
splunk-py-trace-bootstrap
export OTEL_SERVICE_NAME='sentiment'
export OTEL_EXPORTER_OTLP_ENDPOINT='http://myotel:4317'
export OTEL_RESOURCE_ATTRIBUTES='deployment.environment=lab'
splunk-py-trace python sentiment.py

