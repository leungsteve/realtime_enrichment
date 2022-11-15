from flask import Flask, jsonify, request

user_lookup = Flask(__name__)

user_file = '/var/appdata/yelp_academic_dataset_user.json'

@user_lookup.route('/')
def root():
    return jsonify(message='user-lookup-api: please access /lookup_user')

@user_lookup.route('/user_lookup')
def lookup_user2():
    user_id = request.args.get('user_id')
    search_user = '{"user_id":"' + user_id
    searchfile = open(user_file, "r")
    for line in searchfile:
        if search_user in line:
            return line
    searchfile.close()
    return jsonify(message="user not found"), 404

if __name__ == "__main__":
    user_lookup.run(host ='0.0.0.0', port = 5003, debug = False)
