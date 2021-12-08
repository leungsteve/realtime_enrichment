deactivate
source venv/bin/activate
splunk-py-trace-bootstrap
export OTEL_SERVICE_NAME='user-lookup'
export OTEL_EXPORTER_OTLP_ENDPOINT='http://myotel:4317'
export OTEL_RESOURCE_ATTRIBUTES='deployment.environment=lab'
splunk-py-trace python user_lookup.py

