###########################################
################ Metrics ##################
###########################################

data "template_file" "metricbeat" {
  template = file("../util/metricbeat/metricbeat.reference.yml")
  vars = {
      aws_access_key_id = var.aws_access_key_id
      aws_secret_access_key = var.aws_secret_access_key
      cloud_id = ec_deployment.elasticsearch.elasticsearch[0].cloud_id
      cloud_auth = "${ec_deployment.elasticsearch.elasticsearch_username}:${ec_deployment.elasticsearch.elasticsearch_password}"
  }
}

resource "local_file" "metricbeat" {
    depends_on = [ec_deployment.elasticsearch]
    content = data.template_file.metricbeat.rendered
    filename = "../util/metricbeat/metricbeat.yml"
}

resource "null_resource" "setup_metricbeat" {
  depends_on = [local_file.metricbeat]
  provisioner "local-exec" {
    command = "sh setup-metricbeat.sh"
    interpreter = ["bash", "-c"]
    working_dir = "../util"
  }
}
