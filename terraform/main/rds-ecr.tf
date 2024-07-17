resource "aws_ecr_repository" "this" {
  name                 = "softtrainer-backend"
  image_tag_mutability = "MUTABLE"
  force_delete         = false
}

resource "aws_security_group" "rds_psql_sg" {
  name        = "allow-psql"
  description = "Allow access to Postgres from EC2"
  vpc_id      = data.aws_vpcs.default.ids[0]
  ingress {
    description     = "PostgreSQL access from EC2"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2_sg.id]
  }
  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

resource "aws_db_instance" "this" {
  allocated_storage = 20
  identifier        = "database-1"
  engine            = "postgres"
  engine_version    = "16.3"
  instance_class    = "db.t3.micro"
  #   username          = "postgres"
  #   password             = "foobarbaz"
  parameter_group_name   = "default.postgres16"
  skip_final_snapshot    = true
  apply_immediately      = true
  storage_encrypted      = true
  publicly_accessible    = true
  vpc_security_group_ids = [aws_security_group.rds_psql_sg.id]
}
