FROM python:3.10-slim

WORKDIR /app
COPY requirements.txt /app
RUN pip install -r requirements.txt



COPY ./review.py /app

EXPOSE 5000

CMD [ "python", "review.py" ]

#docker build -f Dockerfile -t localhost:8000/review:0.01 .
#docker push localhost:8000/review:0.01
