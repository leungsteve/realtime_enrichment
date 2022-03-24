---
title: "Install K3 and Helm"
linkTitle: "Install K3 and Helm"
weight: 30
draft: false
---


Install K3s and helm. Run the following commands from the VM.
```bash
curl -sfL https://get.k3s.io | sh -
curl -s https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3 | bash
sudo mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown `whoami`. ~/.kube/config
echo 'export KUBECONFIG=~/.kube/config' >> ~/.bashrc
source ~/.bashrc
```
