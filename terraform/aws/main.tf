###########################################
################## AWS ####################
###########################################

provider "aws" {
  region = var.aws_region
}

data "aws_region" "current" {

}

data "aws_availability_zones" "available" {
  state = "available"
}

variable "aws_access_key_id" {
  type = string
}

variable "aws_secret_access_key" {
  type = string
}

###########################################
############ Custom Variables #############
###########################################

resource "random_string" "random_string" {
  length = 8
  special = false
  upper = false
  lower = true
  number = false
}

variable "aws_region" {
  type = string
  default = "us-east-1"
}

variable "alexa_enabled" {
  type = bool
  default = false
}

variable "pacman_players_skill_id" {
  type = string
  default = ""
}

variable "elastic_cloud_api_key" {
  type = string
  default = ""
}

variable "global_prefix" {
  type = string
  default = "streaming-pacman"
}

variable "start_title" {
  type = string
  default = "Streaming Pac-Man with Apache Kafka"
}

variable "blinky_alias" {
  type = string
  default = "broker"
}

variable "pinky_alias" {
  type = string
  default = "partition"
}

variable "inky_alias" {
  type = string
  default = "controller"
}

variable "clyde_alias" {
  type = string
  default = "offset"
}

variable "ksqldb_server_image" {
  type = string
  default = "riferrei/ksqldb-server:latest"
}

variable "redis_sink_image" {
  type = string
  default = "riferrei/redis-sink:latest"
}

variable "functions_metrics_image" {
  type = string
  default = "riferrei/functions-metrics:latest"
}

variable "endpoints_availability_image" {
  type = string
  default = "riferrei/endpoints-availability:latest"
}

variable "metricbeat_image" {
  type = string
  default = "docker.elastic.co/beats/metricbeat:7.15.2"
}
