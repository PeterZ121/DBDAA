/*
Copyright 2021 IBM All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package fullPeerFunc

import (
	"bytes"
	"context"
	"crypto/ecdsa"
	"crypto/sha256"
	"crypto/x509"
	"encoding/json"
	"encoding/pem"
	"errors"
	"fmt"
	"math/big"
	"net"
	"os"
	"path/filepath"
	"crypto/rsa"
	"strings"
//	"time"

	"github.com/hyperledger/fabric-gateway/pkg/client"
	"github.com/hyperledger/fabric-gateway/pkg/identity"
	"github.com/hyperledger/fabric-protos-go-apiv2/gateway"
	"google.golang.org/grpc/status"
)

// HandleConnection handles the incoming connection from the light node
func HandleConnection(conn net.Conn, contract *client.Contract) {
	defer conn.Close()
	

	// Receive and parse the message from the light node
	buffer := make([]byte, 4096)
	n, err := conn.Read(buffer)
	if err != nil {
		fmt.Printf("Failed to read message: %v\n", err)
		return
	}

	// Deserialize the received message into map[string]string
	var message map[string]string
	err = json.Unmarshal(buffer[:n], &message)
	if err != nil {
		fmt.Printf("Failed to unmarshal message: %v\n", err)
		return
	}
	fmt.Printf("Received message: %v\n", message)

	// Get the certificate and signature
	certString := message["certificate"]      // Certificate string
	_ = []byte(message["signature"]) // Signature (assumed to be appropriately formatted)

	// Verify the identity of the node
/*	pubKey, err := loadPublicKey(certString)
	if err != nil {
		fmt.Printf("Failed to load public key: %v\n", err)
		return
	}

	// The message to be verified (assuming the signed message is in message["signedMessage"])
	messageToVerify := []byte(message["signedMessage"])

	// Verify the signature
	if !verifySignature(pubKey, messageToVerify, signature) {
		fmt.Printf("Signature verification failed\n")
		return
	}
*/
	// Verify that the organization in the certificate is org6
	cert, err := loadCertificateFromString(certString)
	if err != nil {
		fmt.Printf("Failed to load certificate: %v\n", err)
		return
	}

	if !containsOrg(cert.Subject.String(), "org6") {
		fmt.Printf("Unauthorized organization, subject does not contain Org6: %s\n", cert.Subject.String())
		return
	}

	fmt.Println("Signature and organization verification succeeded. Proceeding with operation...")

	// Determine which chaincode function to execute based on message["operation"]
	operation := message["operation"]
	var response map[string]string

	switch operation {
	case "UploadDeviceInfo":
		DDID := message["DDID"]
		pk := message["pk"]
		_, err := contract.SubmitTransaction("UploadDeviceInfo", DDID, pk)
		if err != nil {
			response = map[string]string{
				"status": "failure",
				"detail": fmt.Sprintf("Failed to upload device info: %v", err),
			}
		} else {
			response = map[string]string{
				"status": "success",
				"detail": "Device information uploaded successfully",
			}
		}

	case "QueryFromDIT":
		DDID := message["DDID"]
		evaluateResult, err := contract.EvaluateTransaction("QueryFromDIT", DDID)
		if err != nil {
			response = map[string]string{
				"status": "failure",
				"detail": fmt.Sprintf("Failed to query device info: %v", err),
			}
		} else {
			response = map[string]string{
				"status": "success",
				"pk":     string(evaluateResult),
			}
		}

	case "UploadRealUserInfo":
		MDID := message["MDID"]
		pk := message["pk"]
		merkleRoot := message["merkleRoot"]
		_, err := contract.SubmitTransaction("UploadRealUserInfo", MDID, pk, merkleRoot)
		if err != nil {
			response = map[string]string{
				"status": "failure",
				"detail": fmt.Sprintf("Failed to upload real user info: %v", err),
			}
		} else {
			response = map[string]string{
				"status": "success",
				"detail": "Real user information uploaded successfully",
			}
		}

	case "QueryFromRUIT":
		MDID := message["MDID"]
		evaluateResult, err := contract.EvaluateTransaction("QueryFromRUIT", MDID)
		if err != nil {
			response = map[string]string{
				"status": "failure",
				"detail": fmt.Sprintf("Failed to query real user info: %v", err),
			}
		} else {
			result := formatJSON(evaluateResult)
			response = map[string]string{
				"status":      "success",
				"userDetails": result,
			}
		}

	case "UploadAnonUserInfo":
		ADID := message["ADID"]
		pk := message["pk"]
		hM := message["hM"]
		_, err := contract.SubmitTransaction("UploadAnonUserInfo", ADID, pk, hM)
		if err != nil {
			response = map[string]string{
				"status": "failure",
				"detail": fmt.Sprintf("Failed to upload anonymous user info: %v", err),
			}
		} else {
			response = map[string]string{
				"status": "success",
				"detail": "Anonymous user information uploaded successfully",
			}
		}

	case "QueryFromAUIT":
		ADID := message["ADID"]
		evaluateResult, err := contract.EvaluateTransaction("QueryFromAUIT", ADID)
		if err != nil {
			response = map[string]string{
				"status": "failure",
				"detail": fmt.Sprintf("Failed to query anonymous user info: %v", err),
			}
		} else {
			result := formatJSON(evaluateResult)
			response = map[string]string{
				"status":      "success",
				"userDetails": result,
			}
		}

	default:
		response = map[string]string{
			"status": "failure",
			"detail": fmt.Sprintf("Unknown operation: %s", operation),
		}
	}

	// Serialize the response to JSON and send it back to the light node
	jsonResponse, err := json.Marshal(response)
	if err != nil {
		fmt.Printf("Failed to marshal response: %v\n", err)
		return
	}
	_, err = conn.Write(jsonResponse)
	if err != nil {
		fmt.Printf("Failed to send response: %v\n", err)
	}
}

func containsOrg(subject string, org string) bool {
	return strings.Contains(subject, org)
}

// loadPublicKey 从证书或 PEM 格式的公钥中加载公钥，支持 ECDSA 和 RSA 公钥
func loadPublicKey(certOrKeyString string) (interface{}, error) {
	// 解码 PEM 格式的证书或公钥
	block, _ := pem.Decode([]byte(certOrKeyString))
	if block == nil {
		return nil, fmt.Errorf("failed to decode PEM block")
	}

	// 如果是证书类型，则使用 ParseCertificate 解析 X.509 证书
	if block.Type == "CERTIFICATE" {
		cert, err := x509.ParseCertificate(block.Bytes)
		if err != nil {
			return nil, fmt.Errorf("failed to parse certificate: %v", err)
		}

		// 提取公钥，并根据类型处理
		switch pubKey := cert.PublicKey.(type) {
		case *ecdsa.PublicKey:
			return pubKey, nil
		case *rsa.PublicKey:
			return pubKey, nil
		default:
			return nil, fmt.Errorf("unsupported public key type in certificate")
		}
	}

	// 如果是公钥类型，则使用 ParsePKIXPublicKey 解析 PKCS#8 格式的公钥
	if block.Type == "PUBLIC KEY" {
		pubKey, err := x509.ParsePKIXPublicKey(block.Bytes)
		if err != nil {
			return nil, fmt.Errorf("failed to parse PKIX public key: %v", err)
		}

		// 根据类型处理公钥
		switch pubKey := pubKey.(type) {
		case *ecdsa.PublicKey:
			return pubKey, nil
		case *rsa.PublicKey:
			return pubKey, nil
		default:
			return nil, fmt.Errorf("unsupported public key type in PKIX public key")
		}
	}

	return nil, fmt.Errorf("unsupported PEM block type: %s", block.Type)
}

// verifySignature verifies the signature from the light node
func verifySignature(pubKey *ecdsa.PublicKey, message []byte, signature []byte) bool {
	// Compute the hash of the message
	hash := sha256.Sum256(message)

	// Split the signature into r and s
	rLen := len(signature) / 2
	r := new(big.Int).SetBytes(signature[:rLen])
	s := new(big.Int).SetBytes(signature[rLen:])

	// Verify the signature
	return ecdsa.Verify(pubKey, hash[:], r, s)
}

// loadCertificateFromString loads the certificate from a string
func loadCertificateFromString(certString string) (*x509.Certificate, error) {
	// Decode the PEM-formatted certificate
	block, _ := pem.Decode([]byte(certString))
	if block == nil || block.Type != "CERTIFICATE" {
		return nil, fmt.Errorf("failed to decode PEM block containing the certificate")
	}

	// Parse the X.509 certificate
	cert, err := x509.ParseCertificate(block.Bytes)
	if err != nil {
		return nil, fmt.Errorf("failed to parse certificate: %v", err)
	}

	return cert, nil
}

func loadCertificate(filename string) (*x509.Certificate, error) {
	certificatePEM, err := os.ReadFile(filename)
	if err != nil {
		return nil, fmt.Errorf("failed to read certificate file: %w", err)
	}
	return identity.CertificateFromPEM(certificatePEM)
}

// UploadDeviceInfo uploads device information to the alliance blockchain
func UploadDeviceInfo(contract *client.Contract, DDID string, pk string) string {
	res, err := contract.SubmitTransaction("UploadDeviceInfo", DDID, pk)
	if err != nil {
		exampleErrorHandling(err)
		return ""
	}
	return string(res)
}

// QueryFromDIT queries device information from the alliance blockchain by DDID
func QueryFromDIT(contract *client.Contract, DDID string) string {
	res, err := contract.EvaluateTransaction("QueryFromDIT", DDID)
	if err != nil {
		exampleErrorHandling(err)
		return ""
	}
	return string(res)
}

// UploadRealUserInfo uploads user's real identity information to the private blockchain
func UploadRealUserInfo(contract *client.Contract, MDID string, pk string, merkleRoot string) string {
	res, err := contract.SubmitTransaction("UploadRealUserInfo", MDID, pk, merkleRoot)
	if err != nil {
		exampleErrorHandling(err)
		return ""
	}
	return string(res)
}

// QueryFromRUIT queries user's real identity information by MDID from the private blockchain
func QueryFromRUIT(contract *client.Contract, MDID string) []string {
	res, err := contract.EvaluateTransaction("QueryFromRUIT", MDID)
	if err != nil {
		exampleErrorHandling(err)
		return nil
	}

	// Assuming response is a comma-separated string, split it
	result := strings.Split(string(res), ",")
	return result
}

// UploadAnonUserInfo uploads user's anonymous information to the alliance blockchain
func UploadAnonUserInfo(contract *client.Contract, ADID string, pk string, hM string) string {
	res, err := contract.SubmitTransaction("UploadAnonUserInfo", ADID, pk, hM)
	if err != nil {
		exampleErrorHandling(err)
		return ""
	}
	return string(res)
}

// QueryFromAUIT queries user's anonymous information by ADID from the alliance blockchain
func QueryFromAUIT(contract *client.Contract, ADID string) []string {
	res, err := contract.EvaluateTransaction("QueryFromAUIT", ADID)
	if err != nil {
		exampleErrorHandling(err)
		return nil
	}

	// Assuming response is a comma-separated string, split it
	result := strings.Split(string(res), ",")
	return result
}


// formatJSON formats raw JSON byte data for easier reading
func formatJSON(data []byte) string {
	var prettyJSON bytes.Buffer
	err := json.Indent(&prettyJSON, data, "", "    ")
	if err != nil {
		return string(data)
	}
	return prettyJSON.String()
}

// exampleErrorHandling demonstrates how to handle errors from transactions
func exampleErrorHandling(err error) {
	fmt.Println("\n--> start exampleErrorHandling")
	fmt.Println("*** Successfully caught the error:")

	switch err := err.(type) {
	case *client.EndorseError:
		fmt.Printf("Endorse error for transaction %s with gRPC status %v: %s\n", err.TransactionID, status.Code(err), err)
	case *client.SubmitError:
		fmt.Printf("Submit error for transaction %s with gRPC status %v: %s\n", err.TransactionID, status.Code(err), err)
	case *client.CommitStatusError:
		if errors.Is(err, context.DeadlineExceeded) {
			fmt.Printf("Timeout waiting for transaction %s commit status: %s", err.TransactionID, err)
		} else {
			fmt.Printf("Error obtaining commit status for transaction %s with gRPC status %v: %s\n", err.TransactionID, status.Code(err), err)
		}
	case *client.CommitError:
		fmt.Printf("Transaction %s failed to commit with status %d: %s\n", err.TransactionID, int32(err.Code), err)
	default:
		panic(fmt.Errorf("unexpected error type %T: %w", err, err))
	}

	// Any error that originates from a peer or orderer node external to the gateway will have its details
	// embedded within the gRPC status error. The following code shows how to extract that.
	statusErr := status.Convert(err)

	details := statusErr.Details()
	if len(details) > 0 {
		fmt.Println("Error Details:")

		for _, detail := range details {
			switch detail := detail.(type) {
			case *gateway.ErrorDetail:
				fmt.Printf("- address: %s, mspId: %s, message: %s\n", detail.Address, detail.MspId, detail.Message)
			}
		}
	}
}

// applyApprove connects to the notary node and sends a message, then reads the response
func applyApprove(notaryIp string, message string) string {
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
func applyApprove2(notaryIp string, message map[string]string) map[string]string {
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
	if orgNum > 2 {
		peerEndpoint = fmt.Sprintf("localhost:1%d%02d1", orgNum,peerNum)
	}else {
		peerEndpoint = fmt.Sprintf("localhost:%d051", orgNum*2+5)
	}
	

	// Generate gateway peer
	gatewayPeer = fmt.Sprintf("peer%d.org%d.example.com", peerNum,orgNum)

	// Return the generated variables
	return mspID, cryptoPath, certPath, keyPath, tlsCertPath, peerEndpoint, gatewayPeer
}


