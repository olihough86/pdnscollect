package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strings"

	"github.com/elastic/go-elasticsearch/v8"
	"github.com/elastic/go-elasticsearch/v8/esapi"
	"github.com/gorilla/mux"
)

type DNSData struct {
	IPAddress string `json:"ip_address"`
	Domain    string `json:"domain"`
	Timestamp string `json:"timestamp"`
}

var esClient *elasticsearch.Client

func init() {
	var err error
	esClient, err = elasticsearch.NewDefaultClient()
	if err != nil {
		log.Fatalf("Error initializing Elasticsearch client: %s", err)
	}
}

var dnsDataList []DNSData

func submitDNSData(w http.ResponseWriter, r *http.Request) {
	var newDNSData DNSData
	json.NewDecoder(r.Body).Decode(&newDNSData)

	// Log received DNS data to the terminal
	log.Printf("Received DNS data: IP=%s, Domain=%s,Timestamp=%s", newDNSData.IPAddress, newDNSData.Domain, newDNSData.Timestamp)

	// Convert the DNS data struct to JSON
	jsonData, err := json.Marshal(newDNSData)
	if err != nil {
		log.Printf("Error converting DNS data to JSON: %s", err)
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	// Index the DNS data in Elasticsearch
	req := esapi.IndexRequest{
		Index:      "dnsdata",
		Body:       strings.NewReader(string(jsonData)),
		Refresh:    "true",
	}

	res, err := req.Do(r.Context(), esClient)
	if err != nil {
		log.Printf("Error sending DNS data to Elasticsearch: %s", err)
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	defer res.Body.Close()

	if res.IsError() {
		errMsg := fmt.Sprintf("Elasticsearch error: %s", res.String())
		log.Printf(errMsg)
		http.Error(w, errMsg, http.StatusInternalServerError)
		return
	}

	json.NewEncoder(w).Encode(newDNSData)
}

func main() {
	router := mux.NewRouter().StrictSlash(true)
	router.HandleFunc("/api/dnsdata", submitDNSData).Methods("POST")

	log.Fatal(http.ListenAndServe(":8080", router))
}
