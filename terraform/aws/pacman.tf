###########################################
################## HTML ###################
###########################################

/* Uncomment this version for unique bucket
data "template_file" "bucket_pacman" {
  template = "${var.global_prefix}-${random_string.random_string.result}"
}
*/

data "template_file" "bucket_pacman" {
  template = var.global_prefix
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

data "template_file" "index" {
  template = file("../../pacman/index.html")
  vars = {
    start_title = var.start_title
  }
}

resource "aws_s3_bucket_object" "index" {
  bucket = aws_s3_bucket.pacman.bucket
  key = "index.html"
  content_type = "text/html"
  content = data.template_file.index.rendered
}

resource "aws_s3_bucket_object" "error" {
  bucket = aws_s3_bucket.pacman.bucket
  key = "error.html"
  content_type = "text/html"
  source = "../../pacman/error.html"
}

data "template_file" "start" {
  template = file("../../pacman/start.html")
  vars = {
    apm_server_url = ec_deployment.elasticsearch.apm[0].https_endpoint
    start_title = var.start_title
    blinky_alias = var.blinky_alias
    pinky_alias = var.pinky_alias
    inky_alias = var.inky_alias
    clyde_alias = var.clyde_alias
  }
}

resource "aws_s3_bucket_object" "start" {
  depends_on = [ec_deployment.elasticsearch]
  bucket = aws_s3_bucket.pacman.bucket
  key = "start.html"
  content_type = "text/html"
  content = data.template_file.start.rendered
}

resource "aws_s3_bucket_object" "webmanifest" {
  bucket = aws_s3_bucket.pacman.bucket
  key = "site.webmanifest"
  content_type = "application/manifest+json"
  source = "../../pacman/site.webmanifest"
}

data "template_file" "scoreboard" {
  template = file("../../pacman/scoreboard.html")
  vars = {
    start_title = var.start_title
    apm_server_url = ec_deployment.elasticsearch.apm[0].https_endpoint
    scoreboard_api = "${aws_api_gateway_deployment.scoreboard_v1.invoke_url}${aws_api_gateway_resource.scoreboard_resource.path}"
  }
}

resource "aws_s3_bucket_object" "scoreboard" {
  bucket = aws_s3_bucket.pacman.bucket
  key = "scoreboard.html"
  content_type = "text/html"
  content = data.template_file.scoreboard.rendered
}

###########################################
################### CSS ###################
###########################################

resource "aws_s3_bucket_object" "css_files" {
  for_each = fileset(path.module, "../../pacman/game/css/*.*")
  bucket = aws_s3_bucket.pacman.bucket
  key = replace(each.key, "../../pacman/", "")
  content_type = "text/css"
  source = each.value
}

###########################################
################### IMG ###################
###########################################

resource "aws_s3_bucket_object" "img_files" {
  for_each = fileset(path.module, "../../pacman/game/img/*.*")
  bucket = aws_s3_bucket.pacman.bucket
  key = replace(each.key, "../../pacman/", "")
  content_type = "images/png"
  source = each.value
}

###########################################
################### JS ####################
###########################################

resource "aws_s3_bucket_object" "js_files" {
  for_each = fileset(path.module, "../../pacman/game/js/*.*")
  bucket = aws_s3_bucket.pacman.bucket
  key = replace(each.key, "../../pacman/", "")
  content_type = "text/javascript"
  source = each.value
}

data "template_file" "shared_js" {
  template = file("../../pacman/game/js/shared.js")
  vars = {
    event_handler_api = "${aws_api_gateway_deployment.event_handler_v1.invoke_url}${aws_api_gateway_resource.event_handler_resource.path}"
    scoreboard_api = "${aws_api_gateway_deployment.scoreboard_v1.invoke_url}${aws_api_gateway_resource.scoreboard_resource.path}"
  }
}

resource "aws_s3_bucket_object" "shared_js" {
  depends_on = [aws_s3_bucket_object.js_files]
  bucket = aws_s3_bucket.pacman.bucket
  key = "game/js/shared.js"
  content_type = "text/javascript"
  content = data.template_file.shared_js.rendered
}

###########################################
################# Sounds ##################
###########################################

resource "aws_s3_bucket_object" "snd_files" {
  for_each = fileset(path.module, "../../pacman/game/sound/*.*")
  bucket = aws_s3_bucket.pacman.bucket
  key = replace(each.key, "../../pacman/", "")
  content_type = "audio/mpeg"
  source = each.value
}
