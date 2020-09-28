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
