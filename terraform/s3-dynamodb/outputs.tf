output "s3_state_bucket_name" {
  value = module.s3_tfstate.s3_bucket_id
}

output "dynamodb_lock_name" {
  value = aws_dynamodb_table.state_lock.name
}