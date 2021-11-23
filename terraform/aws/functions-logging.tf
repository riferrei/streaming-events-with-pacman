###########################################
########### Functions Logging #############
###########################################

resource "aws_iam_role" "functions_logging" {
  name = "functions_logging_role"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow"
    }
  ]
}
EOF
}

resource "aws_iam_role_policy" "functions_logging" {
  role = aws_iam_role.functions_logging.name
  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cloudformation:CreateStack",
        "cloudformation:DeleteStack",
        "cloudformation:DescribeStacks",
        "cloudformation:DescribeStackEvents",
        "cloudformation:DescribeStackResources",
        "cloudformation:GetTemplate",
        "cloudformation:UpdateStack",
        "cloudformation:ValidateTemplate",
        "iam:CreateRole",
        "iam:DeleteRole",
        "iam:DeleteRolePolicy",
        "iam:GetRole",
        "iam:GetRolePolicy",
        "iam:PassRole",
        "iam:PutRolePolicy",
        "lambda:AddPermission",
        "lambda:CreateFunction",
        "lambda:DeleteFunction",
        "lambda:GetFunction",
        "lambda:GetFunctionConfiguration",
        "lambda:PutFunctionConcurrency",
        "lambda:RemovePermission",
        "lambda:UpdateFunctionCode",
        "lambda:UpdateFunctionConfiguration",
        "logs:CreateLogGroup",
        "logs:DeleteLogGroup",
        "logs:DeleteSubscriptionFilter",
        "logs:DescribeLogGroups",
        "logs:PutSubscriptionFilter",
        "s3:CreateBucket",
        "s3:DeleteObject",
        "s3:ListBucket",
        "s3:PutObject",
        "s3:GetObject"
      ],
      "Resource": "*"
    }
  ]
}
POLICY
}

data "template_file" "functions_logging" {
  template = file("../util/functions-logging-template.yml")
  vars = {
      functions_logging_role_arn = aws_iam_role.functions_logging.arn
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
