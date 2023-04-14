package main

import (
	"encoding/json"
	"log"
	"net/http"

	"github.com/gorilla/mux"
)

type DNSData struct {
	ID        string `json:"id"`
	IPAddress string `json:"ip_address"`
	Domain    string `json:"domain"`
}

var dnsDataList []DNSData

func submitDNSData(w http.ResponseWriter, r *http.Request) {
	var newDNSData DNSData
	json.NewDecoder(r.Body).Decode(&newDNSData)
	dnsDataList = append(dnsDataList, newDNSData)

	// Log received DNS data to the terminal
	log.Printf("Received DNS data: ID=%s, IP=%s, Domain=%s", newDNSData.ID, newDNSData.IPAddress, newDNSData.Domain)

	json.NewEncoder(w).Encode(newDNSData)
}

func main() {
	router := mux.NewRouter().StrictSlash(true)
	router.HandleFunc("/api/dnsdata", submitDNSData).Methods("POST")

	log.Fatal(http.ListenAndServe(":8080", router))
}
