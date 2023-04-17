document.getElementById('save').addEventListener('click', function () {
    const keywords = document.getElementById('keywords').value;
    chrome.storage.sync.set({ keywords: keywords }, function () {
      console.log('Keywords saved:', keywords);
    });
  });
  
  chrome.storage.sync.get('keywords', function (data) {
    if (data.keywords) {
      document.getElementById('keywords').value = data.keywords;
    }
  });
  