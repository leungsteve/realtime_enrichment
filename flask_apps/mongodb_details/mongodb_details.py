from flask import Flask, jsonify
from pymongo import MongoClient
import datetime
# run "flask run" in project root directory to start flask app

mongodbdetails = Flask(__name__)

@mongodbdetails.route('/')
def root():
    return jsonify(message='user-lookup-api: please access /O11yCollection_count')

@mongodbdetails.route('/O11yCollection_count')
def mongodb_details():
    Client = MongoClient()
    myclient = MongoClient('mongodb', 27017)
    my_database = myclient["O11y"]
    my_collection = my_database["O11yCollection"]
    # number of documents in the collection
    total_count = my_collection.count_documents({})
    return jsonify(message=datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S") + ": Total number of documents : " + str(total_count)), 200

if __name__ == "__main__":
    mongodbdetails.run(host ='0.0.0.0', port = 6000, debug = False)