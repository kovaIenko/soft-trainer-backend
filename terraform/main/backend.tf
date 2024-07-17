terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.50"
    }
  }
  backend "s3" {
    bucket         = "softtrainer-backend-tfstate"
    dynamodb_table = "softtrainer-backend-lock"
    key            = "state/terraform.tfstate"
    region         = "eu-north-1"
  }

  required_version = ">= 1.9"
}

provider "aws" {
  region = "eu-north-1"
}
