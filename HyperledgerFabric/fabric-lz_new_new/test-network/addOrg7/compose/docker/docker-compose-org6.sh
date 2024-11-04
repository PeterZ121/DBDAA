#!/bin/bash

# 输出的文件名
output_file="docker-compose-org6.yaml"

# 写入 YAML 文件的开头部分
cat <<EOL > $output_file
version: '3.7'

networks:
  test:
    name: fabric_test

services:
EOL

# 生成50个org6的peer节点配置
for i in $(seq 0 49); do
  cat <<EOL >> $output_file
  peer${i}.org6.example.com:
    container_name: peer${i}.org6.example.com
    image: hyperledger/fabric-peer:latest
    labels:
      service: hyperledger-fabric
    environment:
      # Generic peer variables
      - CORE_VM_ENDPOINT=unix:///host/var/run/docker.sock
      - CORE_VM_DOCKER_HOSTCONFIG_NETWORKMODE=fabric_test
    volumes:
      - ./docker/peercfg:/etc/hyperledger/peercfg
      - \${DOCKER_SOCK}/:/host/var/run/docker.sock

EOL
done

echo "生成完成: $output_file"

