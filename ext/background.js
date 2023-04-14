// Helper function to send DNS data to the API
function sendDNSDataToAPI(dnsData) {
    const apiURL = 'http://localhost:8080/api/dnsdata'; // Replace with your API URL
  
    fetch(apiURL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(dnsData)
    })
      .then(response => response.json())
      .catch(error => console.error('Error submitting DNS data:', error));
  }
  
  // A Set to store unique hosts
  const uniqueHosts = new Set();
  
  // Listener for DNS requests
  chrome.webRequest.onCompleted.addListener(
    function (details) {
      const domain = new URL(details.url).hostname;
  
      // Check if the domain is already processed
      if (!uniqueHosts.has(domain)) {
        uniqueHosts.add(domain);
  
        const dnsData = {
          id: details.requestId,
          ip_address: details.ip,
          domain: domain
        };
  
        sendDNSDataToAPI(dnsData);
      }
    },
    { urls: ['<all_urls>'] }
  );
  