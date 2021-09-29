terraform {
  required_version = ">= 0.12.29"
  required_providers {
    ec = {
      source = "elastic/ec"
      version = "0.2.1"
    }
  }
}

provider "ec" {
    apikey = var.elastic_cloud_api_key
}

data "ec_stack" "latest" {
  version_regex = "latest"
  region = var.aws_region
}

resource "ec_deployment" "elasticsearch" {
  name = "${var.global_prefix}-elastic-o11y"
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
