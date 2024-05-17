#!/bin/bash

source .env

ssh -i "~/.ssh/aws-kovalenko98.pem" ec2-user@${EC2_IP} "docker stop softtrainer-backend && docker rm softtrainer-backend"

ssh -i "~/.ssh/aws-kovalenko98.pem" ec2-user@${EC2_IP} "docker rmi $(docker images ${AWS_ECR_ENDPOINT}/softtrainer-backend -a -q)"

aws ecr get-login-password --region "${AWS_REGION}" | docker login --username AWS --password-stdin "${AWS_ECR_ENDPOINT}"

ssh -i "~/.ssh/aws-kovalenko98.pem" ec2-user@${EC2_IP} "docker run -d -p 8443:8443 --name softtrainer-backend ${AWS_ECR_ENDPOINT}/softtrainer-backend:latest"

# Check logs on server: ssh -i "~/.ssh/aws-kovalenko98.pem" ec2-user@${EC2_IP} "docker logs softtrainer-backend -f"
#ssh -i "~/.ssh/aws-kovalenko98.pem" ec2-user@ec2-16-171-141-248.eu-north-1.compute.amazonaws.com  "docker logs softtrainer-backend -f"
