/*
Copyright 2021 IBM All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package main

import (
	"crypto/x509"
	"fmt"
	"path"
	"os"
	

	lPF "lightPeerFunc" // 假设你有一个轻节点的包
	"github.com/hyperledger/fabric-gateway/pkg/identity"

	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
)

func main() {
	// Generate dynamic paths and variables for Org6
//	mspID, _, certPath, keyPath, tlsCertPath, peerEndpoint, gatewayPeer := lPF.GenerateDynamicPaths(6, 0)
	_, _, certPath, keyPath, _, _, _ := lPF.GenerateDynamicPaths(6, 0)
	// 假设需要签名的消息
	message := "signedMessage"
	messageBytes := []byte(message)

	// 加载私钥
	privateKey, err := lPF.LoadPrivateKey(keyPath)
	if err != nil {
		fmt.Printf("Failed to load private key: %v\n", err)
		os.Exit(1)
	}

	// 生成签名
	signature, err := lPF.GenerateSignature(privateKey, messageBytes)
	if err != nil {
		fmt.Printf("Failed to generate signature: %v\n", err)
		os.Exit(1)
	}

	// 加载证书
	cert, err := os.ReadFile(certPath)
	if err != nil {
		fmt.Printf("Failed to load certificate: %v\n", err)
		os.Exit(1)
	}

	// 准备发送给重节点的消息
	messageMap := map[string]string{
		"certificate":   string(cert),           // 证书字符串
		"signature":     string(signature),      // 签名字符串
		"signedMessage": message,                // 要验证的消息
		"operation":     "UploadDeviceInfo",     // 假设这是要执行的操作
		"DDID":          "12345",                // 示例数据
		"pk":            "device-public-key",    // 示例数据
	}

	res:=lPF.ApplyApprove2(":7080",messageMap)

	// 打印响应
	fmt.Printf("Response from full peer: %s\n", res)
	
	// 准备发送给重节点的消息
	messageMap = map[string]string{
		"certificate":   string(cert),           // 证书字符串
		"signature":     string(signature),      // 签名字符串
		"signedMessage": message,                // 要验证的消息
		"operation":     "QueryFromDIT",     // 假设这是要执行的操作
		"DDID":          "12345",                // 示例数据
	}

	res=lPF.ApplyApprove2(":7080",messageMap)

	// 打印响应
	fmt.Printf("Response from full peer: %s\n", res)
}

// newGrpcConnection creates a gRPC connection to the Gateway server.
func newGrpcConnection(peerEndpoint string, tlsCertPath string, gatewayPeer string) *grpc.ClientConn {
	certificate, err := loadCertificate(tlsCertPath)
	if err != nil {
		panic(err)
	}

	certPool := x509.NewCertPool()
	certPool.AddCert(certificate)
	transportCredentials := credentials.NewClientTLSFromCert(certPool, gatewayPeer)

	connection, err := grpc.Dial(peerEndpoint, grpc.WithTransportCredentials(transportCredentials))
	if err != nil {
		panic(fmt.Errorf("failed to create gRPC connection: %w", err))
	}

	return connection
}

// newIdentity creates a client identity for this Gateway connection using an X.509 certificate.
func newIdentity(mspID string, certPath string) *identity.X509Identity {
	certificate, err := loadCertificate(certPath)
	if err != nil {
		panic(err)
	}

	id, err := identity.NewX509Identity(mspID, certificate)
	if err != nil {
		panic(err)
	}

	return id
}

// loadCertificate reads and parses an X.509 certificate from a file.
func loadCertificate(filename string) (*x509.Certificate, error) {
	certificatePEM, err := os.ReadFile(filename)
	if err != nil {
		return nil, fmt.Errorf("failed to read certificate file '%s': %w", filename, err)
	}
	return identity.CertificateFromPEM(certificatePEM)
}

// newSign creates a function that generates a digital signature from a message digest using a private key.
func newSign(keyPath string) identity.Sign {
	// Read the private key file from the keystore directory
	files, err := os.ReadDir(keyPath)
	if err != nil {
		panic(fmt.Errorf("failed to read private key directory '%s': %w", keyPath, err))
	}
	if len(files) == 0 {
		panic(fmt.Errorf("no private key file found in directory '%s'", keyPath))
	}
	privateKeyPEM, err := os.ReadFile(path.Join(keyPath, files[0].Name()))
	if err != nil {
		panic(fmt.Errorf("failed to read private key file: %w", err))
	}

	privateKey, err := identity.PrivateKeyFromPEM(privateKeyPEM)
	if err != nil {
		panic(err)
	}

	sign, err := identity.NewPrivateKeySign(privateKey)
	if err != nil {
		panic(err)
	}

	return sign
}


