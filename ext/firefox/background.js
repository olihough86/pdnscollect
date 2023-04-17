const API_URL = 'http://127.0.0.1:8080/api/dnsdata';
const uniqueHosts = new Set();

async function getKeywordList() {
  return new Promise((resolve, reject) => {
    browser.storage.sync.get(['keywordList'], function (result) {
      if (browser.runtime.lastError) {
        reject(browser.runtime.lastError);
      } else {
        resolve(result.keywordList || []);
      }
    });
  });
}

function checkForExclusion(domain, keywordList) {
  return keywordList.some((keyword) => domain.includes(keyword));
}

async function sendDnsData(dnsData) {
	console.log(dnsData)
  try {
    await fetch(API_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(dnsData),
    });
  } catch (error) {
    console.error('Error sending DNS data:', error);
  }
}

async function processRequest(requestDetails) {
  const domain = new URL(requestDetails.url).hostname;
  if (!domain || domain === 'localhost') {
    console.log("No domain found or localhost, skipping request");
    return;
  }

  const keywordList = await getKeywordList();

  if (checkForExclusion(domain, keywordList)) {
    console.log("Excluded domain:", domain);
    return;
  }

  const ip = requestDetails.ip;
  if (!ip || ip === '127.0.0.1') {
    console.log("Local IP found, skipping request");
    return;
  }
  
    if (uniqueHosts.has(domain)) {
    console.log("Already sent data for domain:", domain);
    return;
  }

  const timestamp = new Date().toISOString();
  const dnsData = {
    IP: ip,
    Domain: domain,
    Timestamp: timestamp,
  };

  await sendDnsData(dnsData);
  uniqueHosts.add(domain);
}

browser.webRequest.onCompleted.addListener(
  processRequest,
  { urls: ['<all_urls>'] }
);

