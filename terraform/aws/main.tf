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
  default = "confluentinc/ksqldb-server:0.20.0"
}

variable "redis_sink_image" {
  type = string
  default = "riferrei/redis-sink:latest"
}

variable "metricbeat_image" {
  type = string
  default = "riferrei/metricbeat:latest"
}
