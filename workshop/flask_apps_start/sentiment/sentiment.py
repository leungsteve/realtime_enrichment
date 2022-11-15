from flask import Flask, jsonify, request
import random, sys

sentiment = Flask(__name__)

def get_arg1():
    return arg1

def get_random_sentiment():
    sentiment_opts = ['-1', '0', '1']
    sentiment = (random.choice(sentiment_opts))
    return sentiment

@sentiment.route('/')
def get_sentiment():
    return jsonify(sentiment=get_random_sentiment())

if __name__ == "__main__":
    sentiment.run(host='0.0.0.0', port=5001, debug=False)
