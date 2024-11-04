#!/bin/bash

# 检查是否提供了两个参数
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <orgX> <number_of_peers>"
    exit 1
fi

# 读取参数
ORG_NUM=$1
PEER_COUNT=$2

# 生成 YAML 文件名
YAML_FILE="org${ORG_NUM}-crypto.yaml"

# 创建 YAML 文件的基本结构
cat > $YAML_FILE << EOF
# Copyright IBM Corp. All Rights Reserved.
#
# SPDX-License-Identifier: Apache-2.0
#

# ---------------------------------------------------------------------------
# "PeerOrgs" - Definition of organizations managing peer nodes
# ---------------------------------------------------------------------------
PeerOrgs:
  # ---------------------------------------------------------------------------
  # Org${ORG_NUM}
  # ---------------------------------------------------------------------------
  - Name: Org${ORG_NUM}
    Domain: org${ORG_NUM}.example.com
    EnableNodeOUs: true
    Template:
      Count: ${PEER_COUNT}   #生成证书的数量  => 组织中peer节点的数目
      SANS:
        - localhost
    Users:
      Count: 1  #生成用户证书个数
EOF

echo "YAML file $YAML_FILE has been generated for Org${ORG_NUM} with ${PEER_COUNT} peers."

