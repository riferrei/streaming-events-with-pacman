###########################################
############## Elastic O11y ###############
###########################################

provider "ec" {
    apikey = var.elastic_cloud_api_key
}

data "ec_stack" "latest" {
  version_regex = "latest"
  region = var.aws_region
}

resource "ec_deployment" "elasticsearch" {
  name = "${var.global_prefix}-${random_string.random_string.result}"
  deployment_template_id = "aws-io-optimized-v2"
  region = data.ec_stack.latest.region
  version = data.ec_stack.latest.version
  elasticsearch {
    autoscale = "true"
    topology {
      id = "hot_content"
      size = "29g"
      zone_count = "2"
    }
  }
  kibana {
    topology {
      size = "8g"
      zone_count = "2"
    }
  }
  apm {
    topology {
      size = "32g"
      zone_count = 2
    }
  } 
}
