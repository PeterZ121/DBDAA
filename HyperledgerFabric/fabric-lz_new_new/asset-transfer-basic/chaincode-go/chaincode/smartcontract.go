package chaincode

import (
	"encoding/json"
	"fmt"
	"github.com/hyperledger/fabric-contract-api-go/contractapi"
)

// SmartContract provides functions for managing device and user info on both private and alliance blockchains
type SmartContract struct {
	contractapi.Contract
}

// DeviceInfo defines the structure for device data
type DeviceInfo struct {
	DDID string `json:"DDID"`
	Pk   string `json:"Pk"`
}

// RealUserInfo defines the structure for user real identity data
type RealUserInfo struct {
	MDID      string `json:"MDID"`
	Pk        string `json:"Pk"`
	MerkleRoot string `json:"MerkleRoot"`
}

// AnonUserInfo defines the structure for anonymous user data
type AnonUserInfo struct {
	ADID string `json:"ADID"`
	Pk   string `json:"Pk"`
	HM   string `json:"hM"`
}

// UploadDeviceInfo uploads the device information to the alliance blockchain
func (s *SmartContract) UploadDeviceInfo(ctx contractapi.TransactionContextInterface, DDID string, pk string) (bool, error) {
	deviceInfo := DeviceInfo{
		DDID: DDID,
		Pk:   pk,
	}
	deviceInfoJSON, err := json.Marshal(deviceInfo)
	if err != nil {
		return false, err
	}

	err = ctx.GetStub().PutState(DDID, deviceInfoJSON)
	if err != nil {
		return false, err
	}
	return true, nil
}

// QueryFromDIT retrieves the device public key based on DDID from the alliance blockchain
func (s *SmartContract) QueryFromDIT(ctx contractapi.TransactionContextInterface, DDID string) (string, error) {
	deviceInfoJSON, err := ctx.GetStub().GetState(DDID)
	if err != nil {
		return "", fmt.Errorf("failed to read from world state: %v", err)
	}
	if deviceInfoJSON == nil {
		return "", fmt.Errorf("device info %s does not exist", DDID)
	}

	var deviceInfo DeviceInfo
	err = json.Unmarshal(deviceInfoJSON, &deviceInfo)
	if err != nil {
		return "", err
	}
	return deviceInfo.Pk, nil
}

// UploadRealUserInfo uploads the user's real identity information to the private blockchain
func (s *SmartContract) UploadRealUserInfo(ctx contractapi.TransactionContextInterface, MDID string, pk string, merkleRoot string) (bool, error) {
	userInfo := RealUserInfo{
		MDID:      MDID,
		Pk:        pk,
		MerkleRoot: merkleRoot,
	}
	userInfoJSON, err := json.Marshal(userInfo)
	if err != nil {
		return false, err
	}

	err = ctx.GetStub().PutState(MDID, userInfoJSON)
	if err != nil {
		return false, err
	}
	return true, nil
}

// QueryFromRUIT retrieves the user's first public key and merkle root from the private blockchain based on MDID
func (s *SmartContract) QueryFromRUIT(ctx contractapi.TransactionContextInterface, MDID string) ([]string, error) {
	userInfoJSON, err := ctx.GetStub().GetState(MDID)
	if err != nil {
		return nil, fmt.Errorf("failed to read from world state: %v", err)
	}
	if userInfoJSON == nil {
		return nil, fmt.Errorf("real user info %s does not exist", MDID)
	}

	var userInfo RealUserInfo
	err = json.Unmarshal(userInfoJSON, &userInfo)
	if err != nil {
		return nil, err
	}
	return []string{userInfo.Pk, userInfo.MerkleRoot}, nil
}

// UploadAnonUserInfo uploads the user's anonymous information to the alliance blockchain
func (s *SmartContract) UploadAnonUserInfo(ctx contractapi.TransactionContextInterface, ADID string, pk string, hM string) (bool, error) {
	anonUserInfo := AnonUserInfo{
		ADID: ADID,
		Pk:   pk,
		HM:   hM,
	}
	anonUserInfoJSON, err := json.Marshal(anonUserInfo)
	if err != nil {
		return false, err
	}

	err = ctx.GetStub().PutState(ADID, anonUserInfoJSON)
	if err != nil {
		return false, err
	}
	return true, nil
}

// QueryFromAUIT retrieves the user's second public key and hM from the alliance blockchain based on ADID
func (s *SmartContract) QueryFromAUIT(ctx contractapi.TransactionContextInterface, ADID string) ([]string, error) {
	anonUserInfoJSON, err := ctx.GetStub().GetState(ADID)
	if err != nil {
		return nil, fmt.Errorf("failed to read from world state: %v", err)
	}
	if anonUserInfoJSON == nil {
		return nil, fmt.Errorf("anonymous user info %s does not exist", ADID)
	}

	var anonUserInfo AnonUserInfo
	err = json.Unmarshal(anonUserInfoJSON, &anonUserInfo)
	if err != nil {
		return nil, err
	}
	return []string{anonUserInfo.Pk, anonUserInfo.HM}, nil
}

