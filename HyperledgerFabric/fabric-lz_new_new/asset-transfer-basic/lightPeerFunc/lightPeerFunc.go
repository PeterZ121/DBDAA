/*
Copyright 2021 IBM All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package lightPeerFunc

import (
	
	
	"bytes"

	"encoding/json"

	"fmt"
	"os"
	"crypto/x509"
	"net"
	"path/filepath"
	"crypto/rand"
	"crypto/sha256"
	"crypto/ecdsa"
	"encoding/pem"
	"io/ioutil"
	
	"github.com/hyperledger/fabric-gateway/pkg/identity"


	
	
)

// GenerateSignature 使用私钥对消息生成 ECDSA 签名
func GenerateSignature(privateKey *ecdsa.PrivateKey, message []byte) ([]byte, error) {
	// 计算消息的 SHA-256 哈希
	hash := sha256.Sum256(message)

	// 使用私钥对哈希进行签名
	r, s, err := ecdsa.Sign(rand.Reader, privateKey, hash[:])
	if err != nil {
		return nil, fmt.Errorf("failed to sign message: %v", err)
	}

	// 将 r 和 s 连接起来作为最终的签名
	signature := append(r.Bytes(), s.Bytes()...)

	return signature, nil
}

// LoadPrivateKey 从 keystore 目录加载 ECDSA 私钥，支持 PKCS#8 格式
func LoadPrivateKey(keyPath string) (*ecdsa.PrivateKey, error) {
	// 读取 keystore 目录中的私钥文件
	files, err := ioutil.ReadDir(keyPath)
	if err != nil {
		return nil, fmt.Errorf("failed to read keystore directory: %v", err)
	}

	// 假设 keystore 目录中只有一个私钥文件
	if len(files) != 1 {
		return nil, fmt.Errorf("expected one private key file in keystore, found %d", len(files))
	}
	privateKeyPath := filepath.Join(keyPath, files[0].Name())

	// 读取私钥文件
	keyPEM, err := ioutil.ReadFile(privateKeyPath)
	if err != nil {
		return nil, fmt.Errorf("failed to read private key file: %v", err)
	}

	// 解析 PEM 编码的私钥
	block, _ := pem.Decode(keyPEM)
	if block == nil || block.Type != "PRIVATE KEY" {
		return nil, fmt.Errorf("failed to decode PEM block containing the private key")
	}

	// 尝试使用 ParseECPrivateKey 解析 EC 私钥
	privateKey, err := x509.ParseECPrivateKey(block.Bytes)
	if err == nil {
		return privateKey, nil
	}

	// 如果 EC 私钥解析失败，尝试使用 ParsePKCS8PrivateKey 解析 PKCS#8 私钥
	key, err := x509.ParsePKCS8PrivateKey(block.Bytes)
	if err != nil {
		return nil, fmt.Errorf("failed to parse private key: %v", err)
	}

	// 确保解析结果是 ECDSA 私钥类型
	ecdsaKey, ok := key.(*ecdsa.PrivateKey)
	if !ok {
		return nil, fmt.Errorf("not an ECDSA private key")
	}

	return ecdsaKey, nil
}

func loadCertificate(filename string) (*x509.Certificate, error) {
	certificatePEM, err := os.ReadFile(filename)
	if err != nil {
		return nil, fmt.Errorf("failed to read certificate file: %w", err)
	}
	return identity.CertificateFromPEM(certificatePEM)
}



// This type of transaction would typically only be run once by an application the first time it was started after its
// initial deployment. A new version of the chaincode deployed later would likely not need to run an "init" function.


// Format JSON data
func formatJSON(data []byte) string {
	var prettyJSON bytes.Buffer
	if err := json.Indent(&prettyJSON, data, "", "  "); err != nil {
		panic(fmt.Errorf("failed to parse JSON: %w", err))
	}
	return prettyJSON.String()
}

// applyApprove connects to the notary node and sends a message, then reads the response
func ApplyApprove(notaryIp string, message string) string {
	// Connect to the server
	conn, err := net.Dial("tcp", notaryIp)
	if err != nil {
		fmt.Println("Error connecting:", err)
		return ""
	}
	defer conn.Close()

	// Send to the server
	conn.Write([]byte(message))

	// Receive the server's response
	buffer := make([]byte, 4096)
	n, err := conn.Read(buffer)
	if err != nil {
		fmt.Println("Error reading:", err)
		return ""
	}

	fmt.Println("Server response:", string(buffer[:n]))
	return string(buffer[:n])
}

// applyApprove2 connects to the notary node, sends a message in map format, and returns the response as a map
func ApplyApprove2(notaryIp string, message map[string]string) map[string]string {
	// Initialize return value
	response := make(map[string]string)

	// Connect to the server
	conn, err := net.Dial("tcp", notaryIp)
	if err != nil {
		fmt.Println("Error connecting:", err)
		response["status"] = "NO"
		response["error"] = "Connection failed"
		return response
	}
	defer conn.Close()

	// Send to the server
	jsonData, err := json.Marshal(message)
	if err != nil {
		fmt.Println("Error marshaling message:", err)
		response["status"] = "NO"
		response["error"] = "JSON marshal failed"
		return response
	}

	_, err = conn.Write(jsonData)
	if err != nil {
		fmt.Println("Error sending data:", err)
		response["status"] = "NO"
		response["error"] = "Failed to send data"
		return response
	}

	// Receive the server's response
	buffer := make([]byte, 4096)
	n, err := conn.Read(buffer)
	if err != nil {
		fmt.Println("Error reading:", err)
		response["status"] = "NO"
		response["error"] = "Failed to read response"
		return response
	}

	// Convert the server response to map[string]string
	err = json.Unmarshal(buffer[:n], &response)
	if err != nil {
		fmt.Println("Error unmarshaling response:", err)
		response["status"] = "NO"
		response["error"] = "JSON unmarshal failed"
	}
	return response
}

// GenerateDynamicPaths dynamically generates certificate, private key paths, and other variable values based on orgNum and peerNum, and returns them
func GenerateDynamicPaths(orgNum int, peerNum int) (mspID string, cryptoPath string, certPath string, keyPath string, tlsCertPath string, peerEndpoint string, gatewayPeer string) {
	// Generate MSP ID
	mspID = fmt.Sprintf("Org%dMSP", orgNum)

	// Base path
	cryptoPath = fmt.Sprintf("../../test-network/organizations/peerOrganizations/org%d.example.com", orgNum)

	// Generate certificate path
	certPath = filepath.Join(cryptoPath, fmt.Sprintf("users/User1@org%d.example.com/msp/signcerts/User1@org%d.example.com-cert.pem", orgNum, orgNum))

	// Generate private key path
	keyPath = filepath.Join(cryptoPath, fmt.Sprintf("users/User1@org%d.example.com/msp/keystore/", orgNum))

	// Generate TLS certificate path
	tlsCertPath = filepath.Join(cryptoPath, fmt.Sprintf("peers/peer%d.org%d.example.com/tls/ca.crt",peerNum, orgNum))

	// Generate peer endpoint
	peerEndpoint = fmt.Sprintf("localhost:1%d%02d1", orgNum,peerNum)

	// Generate gateway peer
	gatewayPeer = fmt.Sprintf("peer%d.org%d.example.com", peerNum,orgNum)

	// Return the generated variables
	return mspID, cryptoPath, certPath, keyPath, tlsCertPath, peerEndpoint, gatewayPeer
}


