Here's an example Jenkins job config that I was able to use to get a working multi-node simulation report:

```
#!/bin/bash

set +e
set -x

pushd jenkins-integration

  BUNDLE_PATH=`mktemp -d`

  bundle install --path $BUNDLE_PATH


cat > gatling-config.json << EOF
{
  "ssh-keyfile" : "/home/cprice/.ssh/jenkins_id_rsa",
  "sbtpath" : "/home/cprice/work/gatling_scratch/sbt/sbt-launch-0.12.4.jar",
  "master": {
    "hostname": "perf-bl14",
    "ip": "10.16.150.32",
    "target": "perf-bl14"
  },
  "steps": [
#    "cobbler-provision",
#    {"install": "3.2"},
    {"simulate": {
        "id": "PE30_vanilla_1000",
        "scenario": {
          "run_description": "PE3VanillaCent5 long-running, 1000 instances, 2 repetitions",
          "is_long_running": false,
          "nodes": [
            {
              "node_config": "pe3_vanilla_cent5.json",
              "num_instances": 2,
              "ramp_up_duration_seconds": 10,
              "num_repetitions": 1
            },
            {
              "node_config": "pe3_bigcatalog_cent5.json",
              "num_instances": 2,
              "ramp_up_duration_seconds": 10,
              "num_repetitions": 1
            }
          ]
        }
      }
    }
  ]
}
EOF

  ruby bin/perf gatling-config.json
  result=$?

  rm -rf $BUNDLE_PATH

  exit $result
```