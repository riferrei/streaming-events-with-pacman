###########################################
################### VPC ###################
###########################################

resource "aws_vpc" "default" {
  cidr_block = "10.0.0.0/16"
  enable_dns_hostnames = true
  tags = {
    Name = "${var.global_prefix}-${random_string.random_string.result}"
  }
}

resource "aws_internet_gateway" "default" {
  vpc_id = aws_vpc.default.id
  tags = {
    Name = "${var.global_prefix}-${random_string.random_string.result}"
  }
}

resource "aws_eip" "default" {
  depends_on = [aws_internet_gateway.default]
  vpc = true
  tags = {
    Name = "${var.global_prefix}-${random_string.random_string.result}"
  }
}

resource "aws_nat_gateway" "default" {
  depends_on = [aws_internet_gateway.default]
  allocation_id = aws_eip.default.id
  subnet_id = aws_subnet.public_subnet[0].id
  tags = {
    Name = "${var.global_prefix}-${random_string.random_string.result}"
  }
}

resource "aws_route" "default" {
  route_table_id = aws_vpc.default.main_route_table_id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id = aws_internet_gateway.default.id
}

resource "aws_route_table" "private_route_table" {
  vpc_id = aws_vpc.default.id
  tags = {
    Name = "${var.global_prefix}-private-route-table"
  }
}

resource "aws_route" "private_route_2_internet" {
  route_table_id = aws_route_table.private_route_table.id
  destination_cidr_block = "0.0.0.0/0"
  nat_gateway_id = aws_nat_gateway.default.id
}

resource "aws_route_table_association" "private_subnet_association" {
  count = length(data.aws_availability_zones.available.names)
  subnet_id = element(aws_subnet.private_subnet.*.id, count.index)
  route_table_id = aws_route_table.private_route_table.id
}

resource "aws_route_table_association" "public_subnet_association" {
  count = length(data.aws_availability_zones.available.names)
  subnet_id = element(aws_subnet.public_subnet.*.id, count.index)
  route_table_id = aws_vpc.default.main_route_table_id
}

###########################################
################# Subnets #################
###########################################

variable "private_cidr_blocks" {
  type = list(string)
  default = [
    "10.0.1.0/24",
    "10.0.2.0/24",
    "10.0.3.0/24",
    "10.0.4.0/24",
    "10.0.5.0/24",
    "10.0.6.0/24",
    "10.0.7.0/24",
    "10.0.8.0/24",
  ]
}

variable "public_cidr_blocks" {
  type = list(string)
  default = [
    "10.0.9.0/24",
    "10.0.10.0/24",
    "10.0.11.0/24",
    "10.0.12.0/24",
    "10.0.13.0/24",
    "10.0.14.0/24",
    "10.0.15.0/24",
    "10.0.16.0/24",
  ]
}

resource "aws_subnet" "private_subnet" {
  count = length(data.aws_availability_zones.available.names)
  vpc_id = aws_vpc.default.id
  cidr_block = element(var.private_cidr_blocks, count.index)
  map_public_ip_on_launch = false
  availability_zone = data.aws_availability_zones.available.names[count.index]
  tags = {
    Name = "${var.global_prefix}-private-subnet-${count.index}"
  }
}

resource "aws_subnet" "public_subnet" {
  count = length(data.aws_availability_zones.available.names)
  vpc_id = aws_vpc.default.id
  cidr_block = element(var.public_cidr_blocks, count.index)
  map_public_ip_on_launch = true
  availability_zone = data.aws_availability_zones.available.names[count.index]
  tags = {
    Name = "${var.global_prefix}-public-subnet-${count.index}"
  }
}

resource "aws_subnet" "cache_server_bastion" {
  count = var.cache_server_bastion_enabled == true ? 1 : 0
  vpc_id = aws_vpc.default.id
  cidr_block = "10.0.17.0/24"
  map_public_ip_on_launch = true
  availability_zone = data.aws_availability_zones.available.names[0]
  tags = {
    Name = "${var.global_prefix}-cache-server-bastion"
  }
}

###########################################
############# Security Groups #############
###########################################

resource "aws_security_group" "kafka_cluster" {
  name = "${var.global_prefix}-kafka-cluster"
  description = "Streaming layer using Kafka"
  vpc_id = aws_vpc.default.id
  ingress {
    from_port = 9092
    to_port = 9092
    protocol = "tcp"
    cidr_blocks = var.private_cidr_blocks
  }
  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "cache_server" {
  name = "${var.global_prefix}-cache-server"
  description = "Cache server for the APIs"
  vpc_id = aws_vpc.default.id
  ingress {
    from_port = 6379
    to_port = 6379
    protocol = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }
  dynamic "ingress" {
    for_each = aws_security_group.cache_server_bastion
    content {
      from_port = 6379
      to_port = 6379
      protocol = "tcp"
      security_groups = [ingress.value.id]
    }
  }
  ingress {
    from_port = 6379
    to_port = 6379
    protocol = "tcp"
    cidr_blocks = var.private_cidr_blocks
  }
  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = {
    Name = "${var.global_prefix}-cache-server"
  }
}

resource "aws_security_group" "ecs_tasks" {
  name = "${var.global_prefix}-ecs-tasks"
  description = "Inbound Access from LBR"
  vpc_id = aws_vpc.default.id
  ingress {
    protocol = "tcp"
    from_port = 8088
    to_port = 8088
  }
  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = {
    Name = "${var.global_prefix}-ecs-tasks"
  }
}

resource "aws_security_group" "cache_server_bastion" {
  count = var.cache_server_bastion_enabled == true ? 1 : 0
  name = "${var.global_prefix}-cache-server-bastion"
  description = "Cache Server Bastion"
  vpc_id = aws_vpc.default.id
  ingress {
    from_port = 22
    to_port = 22
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
  tags = {
    Name = "${var.global_prefix}-cache-server-bastion"
  }
}
