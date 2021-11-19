###########################################
################ Logging ##################
###########################################

data "template_file" "functionbeat" {
  template = file("../util/functionbeat/functionbeat.reference.yml")
  vars = {
      functionbeat_bucket_name = "${var.global_prefix}${random_string.random_string.result}-functionbeat"
      cloud_id = ec_deployment.elasticsearch.elasticsearch[0].cloud_id
      cloud_auth = "${ec_deployment.elasticsearch.elasticsearch_username}:${ec_deployment.elasticsearch.elasticsearch_password}"
  }
}

resource "local_file" "functionbeat" {
    depends_on = [ec_deployment.elasticsearch]
    content = data.template_file.functionbeat.rendered
    filename = "../util/functionbeat/functionbeat.yml"
}

resource "null_resource" "deploy_functionbeat" {
  depends_on = [local_file.functionbeat]
  provisioner "local-exec" {
    command = "sh functionbeat-deploy.sh"
    interpreter = ["bash", "-c"]
    working_dir = "../util"
  }
  provisioner "local-exec" {
    command = "sh functionbeat-undeploy.sh"
    interpreter = ["bash", "-c"]
    working_dir = "../util"
    when = destroy
  }
}
