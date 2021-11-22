###########################################
########### Functions Logging #############
###########################################

data "template_file" "functions_logging" {
  template = file("../util/functions-logging-template.yml")
  vars = {
      functionbeat_bucket_name = "${var.global_prefix}${random_string.random_string.result}-functionbeat"
      cloud_id = ec_deployment.elasticsearch.elasticsearch[0].cloud_id
      cloud_auth = "${ec_deployment.elasticsearch.elasticsearch_username}:${ec_deployment.elasticsearch.elasticsearch_password}"
  }
}

resource "local_file" "functions_logging" {
    depends_on = [ec_deployment.elasticsearch]
    content = data.template_file.functions_logging.rendered
    filename = "../util/functionbeat/functionbeat.yml"
}

resource "null_resource" "functions_logging" {
  depends_on = [local_file.functions_logging]
  provisioner "local-exec" {
    command = "sh functions-logging-deploy.sh"
    interpreter = ["bash", "-c"]
    working_dir = "../util"
  }
  provisioner "local-exec" {
    command = "sh functions-logging-undeploy.sh"
    interpreter = ["bash", "-c"]
    working_dir = "../util"
    when = destroy
  }
}
