---
title: "Multipass"
linkTitle: "Setup A local VM with Multipass"
draft: false
weight: 20
---
### Install Multipass

For example:
```bash
brew install multipass
```

### Launch a local Virtual Machine
Create a Multipass VM. We will use `test4cpu8gb` as my VM name throughout this example.
```bash
multipass launch --name test4cpu8gb --cpus 4 --mem 8Gb --disk 32GB
```

### Add the Yelp data to your VM's file system

Mount /var/appdata to the VM to make the Yelp dataset files available to the VM.
```bash
multipass mount /var/appdata test4cpu8gb
```
Launch shell into the vm and verify that `/var/appdata` is mounted.
```bash
multipass shell test4cpu8gb
```

Once in your VM, validate the Yelp data is in the expected path
```bash
ubuntu@test4cpu8gb:~$ ll /var/appdata/yelp*
-rw-r--r-- 1 ubuntu ubuntu  124380583 Jan 28  2021 /var/appdata/yelp_academic_dataset_business.json
-rw-r--r-- 1 ubuntu ubuntu 6936678061 Jan 28  2021 /var/appdata/yelp_academic_dataset_review.json
-rw-r--r-- 1 ubuntu ubuntu 3684505303 Jan 28  2021 /var/appdata/yelp_academic_dataset_user.json
```
