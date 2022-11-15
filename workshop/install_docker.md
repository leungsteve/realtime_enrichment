# [Install Docker](https://docs.docker.com/registry/deploying/)
```
sudo apt-get update
sudo apt-get install \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
  
sudo apt-get update

sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo systemctl enable docker.service

sudo docker run hello-world
```

### optional: add current user to the docker group (make life easier)
```
sudo usermod -aG docker $(whoami)
echo "log off and log back on to run docker commands with out sudo"
```

# [Deploy a registry server](https://docs.docker.com/registry/deploying/)
```
docker run -d -p 5000:8000 --restart=always --name registry registry:2
```
