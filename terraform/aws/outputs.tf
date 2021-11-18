###########################################
################# Outputs #################
###########################################

output "Pacman" {
  value = "http://${aws_s3_bucket.pacman.website_endpoint}"
}

output "ksqlDB" {
  value = "http://${aws_alb.ksqldb_lbr.dns_name}:80"
}

/*
output "elastic_apm_server_url" {
  value = ec_deployment.elasticsearch.apm[0].https_endpoint
}

output "elastic_apm_secret_token" {
  value = ec_deployment.elasticsearch.apm_secret_token
  sensitive = true
}
*/
