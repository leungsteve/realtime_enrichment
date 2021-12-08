from flask import Flask, jsonify, request

business_lookup = Flask(__name__)

business_file = '/var/appdata/yelp_academic_dataset_business.json'

@business_lookup.route('/')
def root():
    return jsonify(message='business-lookup-api: please access /business_lookup')

@business_lookup.route('/business_lookup')
def lookup():
    business_id = request.args.get('business_id')
    print(business_id)
    search_business = '{"business_id":"' + business_id
    searchfile = open(business_file, "r")
    for line in searchfile:
        if search_business in line:
            return line, 200
    searchfile.close()
    return jsonify(message="business not found"), 404

if __name__ == "__main__":
    business_lookup.run(host ='0.0.0.0', port = 5002, debug = False)
