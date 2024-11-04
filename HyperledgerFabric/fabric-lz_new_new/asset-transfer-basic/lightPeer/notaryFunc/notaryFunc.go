/*
Copyright 2021 IBM All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package notaryFunc

import (
	"bytes"
	"context"
	"crypto/x509"
	"encoding/json"
	"errors"
	"fmt"
	"os"

	"time"
	"strconv"
	"net"
	"strings"


	"github.com/hyperledger/fabric-gateway/pkg/client"
	"github.com/hyperledger/fabric-gateway/pkg/identity"
	"github.com/hyperledger/fabric-protos-go-apiv2/gateway"

	"google.golang.org/grpc/status"
	
)


type Asset struct {
	AppraisedValue int    `json:"AppraisedValue"`
	Color          string `json:"Color"`
	ID             string `json:"ID"`
	Owner          string `json:"Owner"`
	Size           int    `json:"Size"`
}

var now = time.Now()
var assetId = fmt.Sprintf("asset%d", now.Unix()*1e3+int64(now.Nanosecond())/1e6)



func HandleConnection(contract *client.Contract) {
	initLedger(contract)
	getAllAssets(contract)
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
func InitLedger(contract *client.Contract) {
	fmt.Printf("\n--> Submit Transaction: InitLedger, function creates the initial set of assets on the ledger \n")

	_, err := contract.SubmitTransaction("InitLedger")
	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}

	fmt.Printf("*** Transaction committed successfully\n")
}

// Evaluate a transaction to query ledger state.
func getAllAssets(contract *client.Contract) {
	fmt.Println("\n--> Evaluate Transaction: GetAllAssets, function returns all the current assets on the ledger")

	evaluateResult, err := contract.EvaluateTransaction("GetAllAssets")
	if err != nil {
		panic(fmt.Errorf("failed to evaluate transaction: %w", err))
	}
	result := formatJSON(evaluateResult)

	fmt.Printf("*** Result:%s\n", result)
}

// Submit a transaction synchronously, blocking until it has been committed to the ledger.
func createAsset(contract *client.Contract, assetID string, color string,size int, owner string, value int) error  {
	fmt.Printf("\n--> Submit Transaction: CreateAsset, creates new asset with ID, Color, Size, Owner and AppraisedValue arguments \n")

	_, err := contract.SubmitTransaction("CreateAsset", assetID, color, "5", owner, "100")
	if err != nil {
		return err
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** %d%d\n",size,value)
	fmt.Printf("*** Transaction committed successfully\n")
	return nil
}

func deleteAsset(contract *client.Contract, assetID string) {
	fmt.Printf("\n--> Submit Transaction: DeleteAsset\n")

	_, err := contract.SubmitTransaction("DeleteAsset", assetID)
	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}

	fmt.Printf("*** Transaction committed successfully\n")
}

// Evaluate a transaction by assetID to query ledger state.
func readAssetByID(contract *client.Contract, assetID string) (evaluateResult []byte) {
	fmt.Printf("\n--> Evaluate Transaction: ReadAsset, function returns asset attributes\n")

	evaluateResult, err := contract.EvaluateTransaction("ReadAsset", assetID)
	if err != nil {
		panic(fmt.Errorf("failed to evaluate transaction: %w", err))
	}
//	result := formatJSON(evaluateResult)
//
//	fmt.Printf("*** Result:%s\n", result)
	return evaluateResult
}

// Submit transaction asynchronously, blocking until the transaction has been sent to the orderer, and allowing
// this thread to process the chaincode response (e.g. update a UI) without waiting for the commit notification
func transferAssetAsync(contract *client.Contract) {
	fmt.Printf("\n--> Async Submit Transaction: TransferAsset, updates existing asset owner")

	submitResult, commit, err := contract.SubmitAsync("TransferAsset", client.WithArguments(assetId, "Mark"))
	if err != nil {
		panic(fmt.Errorf("failed to submit transaction asynchronously: %w", err))
	}

	fmt.Printf("\n*** Successfully submitted transaction to transfer ownership from %s to Mark. \n", string(submitResult))
	fmt.Println("*** Waiting for transaction commit.")

	if commitStatus, err := commit.Status(); err != nil {
		panic(fmt.Errorf("failed to get commit status: %w", err))
	} else if !commitStatus.Successful {
		panic(fmt.Errorf("transaction %s failed to commit with status: %d", commitStatus.TransactionID, int32(commitStatus.Code)))
	}

	fmt.Printf("*** Transaction committed successfully\n")
}

func transferAssetAcossChain(contractFrom *client.Contract ,contractTo *client.Contract ,assetID string ) error {
	fmt.Printf("\n--> Async Submit Transaction: TransferAssetAcossChain, updates existing asset owner")
//	submitResult, commit, err := contract.SubmitAsync("AssetExists", client.WithArguments(assetId, "Mark"))
	//readAsset
	asset := readAssetByID(contractFrom,assetID)
	var data map[string]interface{}
  	if err := json.Unmarshal(bytes.NewBuffer(asset).Bytes(), &data); err != nil {
   		return err
  	}
	//createAsset
	color := data["Color"].(string)
	size := data["Size"].(float64)
	
	owner := data["Owner"].(string)
	value := data["AppraisedValue"].(float64)
	
	err :=  createAsset(contractTo, assetID, color,int(size), owner, int(value))

	//deleteAsset
	deleteAsset(contractFrom,assetID)
	fmt.Printf("*** Transaction committed successfully\n")
	return err
}

// Submit transaction, passing in the wrong number of arguments ,expected to throw an error containing details of any error responses from the smart contract.
func exampleErrorHandling(contract *client.Contract) {
	fmt.Println("\n--> Submit Transaction: UpdateAsset asset70, asset70 does not exist and should return an error")

	_, err := contract.SubmitTransaction("UpdateAsset", "asset70", "blue", "5", "Tomoko", "300")
	if err == nil {
		panic("******** FAILED to return an error")
	}

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

// Format JSON data
func formatJSON(data []byte) string {
	var prettyJSON bytes.Buffer
	if err := json.Indent(&prettyJSON, data, "", "  "); err != nil {
		panic(fmt.Errorf("failed to parse JSON: %w", err))
	}
	return prettyJSON.String()
}


func applyApprove(notaryIp string , message string) string {
	// 连接服务器
	conn, err := net.Dial("tcp", notaryIp)
	if err != nil {
		fmt.Println("Error connecting:", err)
		return ""
	}
	defer conn.Close()

	// 发送到服务器
	conn.Write([]byte(message))

	// 接收服务器的响应
	buffer := make([]byte, 4096)
	n, err := conn.Read(buffer)
	if err != nil {
		fmt.Println("Error reading:", err)
		return ""
	}

	fmt.Println("Server response:", string(buffer[:n]))
	return string(buffer[:n])
}

func createNotary(contract *client.Contract, notaryID string, cert string) error {
	fmt.Printf("\n--> Submit Transaction: CreateNotary\n")

	_, err := contract.SubmitTransaction("CreateNotary", notaryID, cert)
	if err != nil {
		return err
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** Transaction committed successfully\n")
	return nil
}
func ApplyBecomeNotary(contract *client.Contract) error {
	fmt.Printf("\n--> Submit Transaction: ApplyBecomeNotary\n")
	evaluateResult, err := contract.EvaluateTransaction("ApplyBecomeNotary")
	if err != nil {
		return err
		panic(fmt.Errorf("failed to evaluate transaction: %w", err))
	}
	cert := string(evaluateResult)
	msg := make(map[string]string)
	msg["msgtype"] = "applybecomenotary"
	msg["cert"] = cert
	msg["channelID"] = "channel2"
	msg["IP"] = "localhost:16081"
	reply := applyApprove2("localhost:8080", msg)
	if reply != "YES" {
		fmt.Println("shengqingshibai." + reply)
		return nil
	}
	fmt.Println(msg)
	return nil
}
func applyApprove2(notaryIp string, message map[string]string) string {
	// 连接服务器
	conn, err := net.Dial("tcp", notaryIp)
	if err != nil {
		fmt.Println("Error connecting:", err)
		return "NO"
	}
	defer conn.Close()

	// 发送到服务器
	jsonData, err := json.Marshal(message)
	if err != nil {
		fmt.Println("Error:", err)
		return "NO"
	}
	conn.Write([]byte(jsonData))

	// 接收服务器的响应
	buffer := make([]byte, 4096)
	n, err := conn.Read(buffer)
	if err != nil {
		fmt.Println("Error reading:", err)
		return "NO"
	}

	fmt.Println("Server response:", string(buffer[:n]))
	return string(buffer[:n])
}
func updateelect(contract *client.Contract, str string) error {
	fmt.Printf("\n--> Submit Transaction: updateelect\n")
	_, err := contract.SubmitTransaction("updateelect", str)
	if err != nil {
		return err
		panic(fmt.Errorf("failed to submit transaction: %w", err))
	}
	fmt.Printf("*** Transaction committed successfully\n")
	return nil
}
func electNotary(contract *client.Contract, numToElect int) error {
	fmt.Printf("\n--> Submit Transaction: electNotary\n")
	_, err := contract.SubmitTransaction("Elect", strconv.Itoa(numToElect))
	if err != nil {
		panic(fmt.Errorf("failed to submit transaction: %w", err))
		return err	
	}
	fmt.Printf("*** Transaction committed successfully\n")
	return nil
}
