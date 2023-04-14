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
  
  function shouldCollect(domain, keywords) {
    for (const keyword of keywords) {
      if (domain.includes(keyword)) {
        return false;
      }
    }
    return true;
  }
  
  // Listener for DNS requests
  chrome.webRequest.onCompleted.addListener(
    function (details) {
      chrome.storage.sync.get('keywords', function (data) {
        const domain = new URL(details.url).hostname;
        const keywords = (data.keywords || '').split(',').map(k => k.trim()).filter(k => k);
  
        if (!uniqueHosts.has(domain) && shouldCollect(domain, keywords)) {
          uniqueHosts.add(domain);
  
          const dnsData = {
            id: details.requestId,
            ip_address: details.ip,
            domain: domain
          };
  
          sendDNSDataToAPI(dnsData);
        }
      });
    },
    { urls: ['<all_urls>'] }
  );  