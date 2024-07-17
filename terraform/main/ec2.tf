resource "aws_key_pair" "backend" {
  key_name   = "aws-kovalenko"
  public_key = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCFnAOmYgCdqaREZQN4YGR3RiQ4H9zK7+XA4Iqqv3YokW2SEqUZXuvmjgEwjvPtBoH/8IacJXucyAHBKFhjb7D4JDEUQhKrUtb++2/tWI03i9/Erbun7o0XnbZR8v5kCL59fEaEwTtbluzEXdgf5Mb6hoWLTbD0omwgSDjMEZgoqktEuhnxbwsK6t/7HjTT63zGSt9nPNpL5r27rGEFCaLeR8CfheiKMedhf0f34/6O6YdjysxlJAmOLue1rrH0gzk4/9vlhXH6N2hVcbu8flWcyHI4D79HR+A86w8mG4lNUdsb+6O02PQMUx18Sxm6wLIJr5OA3x0F3Z84nLUIiYDD"
}

resource "aws_instance" "backend" {
  # ami                         = data.aws_ami.amazon_linux.id
  ami                         = "ami-0f76a278bc3380848"
  instance_type               = "t3.micro"
  associate_public_ip_address = true
  key_name                    = aws_key_pair.backend.key_name
  vpc_security_group_ids      = [aws_security_group.ec2_sg.id]
  root_block_device {
    volume_size = 20
  }

  tags = {
    Name = var.project
  }

  depends_on = [aws_key_pair.backend]
}

resource "aws_lb" "backend" {
  name               = "st"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.ec2_sg.id]
  subnets            = [for subnet_id in data.aws_subnets.default.ids : subnet_id]
}

resource "aws_lb_target_group" "backend" {
  name     = "https8443"
  port     = 8443
  protocol = "HTTPS"
  vpc_id   = data.aws_vpcs.default.ids[0]
}

resource "aws_lb_listener" "backend" {
  load_balancer_arn = aws_lb.backend.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

resource "aws_security_group" "ec2_sg" {
  name        = "allow-ssh-web"
  description = "Allow access to SSH and WEB"
  vpc_id      = data.aws_vpcs.default.ids[0]
  dynamic "ingress" {
    for_each = [80, 8080, 443, 22]
    iterator = port
    content {
      description = "TLS from VPC"
      from_port   = port.value
      to_port     = port.value
      protocol    = "tcp"
      cidr_blocks = ["0.0.0.0/0"]
    }
  }
  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}
