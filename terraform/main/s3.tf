locals {
  bucket_name = split("-", var.project)[0]
}

module "s3_bucket" {
  source  = "terraform-aws-modules/s3-bucket/aws"
  version = "~> 4.0"

  bucket                   = local.bucket_name
  object_ownership         = "BucketOwnerEnforced"
  attach_policy            = true
  policy                   = data.aws_iam_policy_document.allow_bucket_access.json
  control_object_ownership = true

  restrict_public_buckets = false
  ignore_public_acls      = false
  block_public_policy     = false
  block_public_acls       = false

  cors_rule = [
    {
      allowed_headers = ["*"]
      allowed_methods = ["GET"]
      allowed_origins = [
        "https://test-web-flutter-427fd.web.app",
        "https://app.thesofttrainer.com"
      ]
      expose_headers = []
    }
  ]
}

data "aws_iam_policy_document" "allow_bucket_access" {
  statement {
    principals {
      type        = "*"
      identifiers = ["*"]
    }
    effect = "Allow"
    actions = [
      "s3:ListBucket",
      "s3:GetObject",
      "s3:GetObjectVersion"
    ]
    resources = [
      "arn:aws:s3:::${local.bucket_name}",
      "arn:aws:s3:::${local.bucket_name}/*"
    ]
  }
}
