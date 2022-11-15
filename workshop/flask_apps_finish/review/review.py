from flask import Flask, jsonify
import random
import subprocess

review = Flask(__name__)

review = Flask(__name__)
num_reviews = 8635403
num_reviews = 100000
reviews_file = '/var/appdata/yelp_academic_dataset_review.json'

@review.route('/')
def hello_world():
    return jsonify(message='Hello, you want to hit /get_review. We have ' + str(num_reviews) + ' reviews!')

@review.route('/get_review')
def get_review():
    random_review_int = str(random.randint(1,num_reviews))
    line_num = random_review_int + 'q;d'
    command = ["sed", line_num, reviews_file] # sed "7997242q;d" <file>
    random_review = subprocess.run(command, stdout=subprocess.PIPE, text=True)
    return random_review.stdout

if __name__ == "__main__":
    review.run(host ='0.0.0.0', port = 5000, debug = False)