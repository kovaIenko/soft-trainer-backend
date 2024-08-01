# How to deploy infrastructure via Terraform

1) Install `terraform` CLI tool. \
https://developer.hashicorp.com/terraform/install
2) Add user credentials to `~/.aws/` folder to `credentials` file. OR use `export` of credentials. To properly set credentials, use `Access keys` in AWS access portal, when you log in.
3) First, go to `s3-dynamodb` folder. It is needed to set up `s3`remote backend where state will be stored, as well as `dynamodb` for locking state to avoid simultaneous infrastructure changes.

```bash
terraform init # initialize terraform directory, download dependencies

terraform plan # check draft of what will be created

terraform apply # apply the changes
```

4) Second, go to `main` folder. Adjust `project.auto.tfvars` file if needed. If region will be different, edit it in `backend.tf` file as well.
5) Do the same `init, plan, apply`.
6) Check if everything works. Conntect to instance using SSH and configure it manually.

