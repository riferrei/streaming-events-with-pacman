###########################################
################## AWS ####################
###########################################

provider "aws" {
  region = "us-east-1"
}

data "aws_region" "current" {

}

data "aws_availability_zones" "available" {
  state = "available"
}

resource "random_string" "random_string" {
  length = 8
  special = false
  upper = false
  lower = true
  number = false
}

data "template_file" "bucket_pacman" {
  template = "${var.global_prefix}${random_string.random_string.result}"
}

resource "aws_s3_bucket" "pacman" {
  bucket = data.template_file.bucket_pacman.rendered
  acl = "public-read"
  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "POST"]
    allowed_origins = ["*"]
  }
  policy = <<EOF
{
    "Version": "2008-10-17",
    "Statement": [
        {
            "Sid": "PublicReadGetObject",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:GetObject",
            "Resource": "arn:aws:s3:::${data.template_file.bucket_pacman.rendered}/*"
        }
    ]
}
EOF
  website {
    index_document = "index.html"
    error_document = "error.html"
  }
}

variable "scoreboard_topic" {
  type = string
  default = "SCOREBOARD"
}

###########################################
############ Alexa Variables ##############
###########################################

variable "alexa_enabled" {
  type = bool
  default = false
}

variable "pacman_players_skill_id" {
  type = string
  default = ""
}

###########################################
############ Other Variables ##############
###########################################

variable "global_prefix" {
  type = string
  default = "streaming-pacman"
}

variable "ksqldb_server_image" {
  type = string
  default = "confluentinc/ksqldb-server:0.11.0"
}

variable "redis_sink_image" {
  type = string
  default = "riferrei/redis-sink:latest"
}
