#!/bin/bash

# set -e

source .env

ssh -i "~/.ssh/aws-kovalenko98.pem" ec2-user@${EC2_IP} "docker stop softtrainer-backend && docker rm softtrainer-backend"
ssh -i "~/.ssh/aws-kovalenko98.pem" ec2-user@${EC2_IP} "docker run -d -p 8443:8443 --name softtrainer-backend ${AWS_ECR_ENDPOINT}/softtrainer-backend:latest"
# ssh -i "aws-kovalenko98.pem" ec2-user@${EC2_IP} ""