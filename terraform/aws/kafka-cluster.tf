resource "aws_kms_key" "kafka_cluster_kms_key" {
  description = "Key for Apache Kafka"
}

resource "aws_cloudwatch_log_group" "kafka_cluster_log_group" {
  name = "kafka_broker_logs"
}

resource "aws_msk_cluster" "kafka_cluster" {

  cluster_name = "${var.global_prefix}-kafka-cluster"
  kafka_version = "2.8.0"
  number_of_broker_nodes = 3

  broker_node_group_info {
    instance_type = "kafka.m5.large"
    ebs_volume_size = 1000
    client_subnets = [aws_subnet.private_subnet[0].id,
                      aws_subnet.private_subnet[1].id,
                      aws_subnet.private_subnet[2].id]
    security_groups = [aws_security_group.kafka_cluster.id]
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "PLAINTEXT"
    }
    encryption_at_rest_kms_key_arn = aws_kms_key.kafka_cluster_kms_key.arn
  }

  open_monitoring {
    prometheus {
      jmx_exporter {
        enabled_in_broker = true
      }
      node_exporter {
        enabled_in_broker = true
      }
    }
  }

  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled = true
        log_group = aws_cloudwatch_log_group.kafka_cluster_log_group.name
      }
    }
  }

  tags = {
    name = "${var.global_prefix}-kafka-cluster"
  }

}
