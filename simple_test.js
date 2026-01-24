const { chromium } = require('playwright');

async function simpleTest() {
  console.log('\n=== SIMPLE PLAYWRIGHT TEST ===');
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  let allMessages = [];
  page.on('console', msg => {
    const text = msg.text();
    allMessages.push({ type: msg.type(), text });
    console.log(`[${msg.type()}] ${text}`);
  });
  page.on('pageerror', err => console.error('[ERROR]', err.message));

  let failedRequests = [];
  page.on('response', response => {
    if (response.status() >= 400) {
      const url = response.url();
      failedRequests.push({ status: response.status(), url });
      console.error(`[HTTP ${response.status()}] ${url}`);
    }
  });

  try {
    console.log('\nLoading page...');
    await page.goto('http://localhost:8080/STARSECTOR_V6J_FINAL_WORKING.html', { waitUntil: 'domcontentloaded', timeout: 10000 });
    console.log('Page loaded!\n');

    await page.waitForTimeout(2000);

    console.log('\n=== ALL CONSOLE MESSAGES ===');
    allMessages.forEach((m, i) => console.log(`${i+1}. [${m.type}] ${m.text}`));
    console.log('=== END MESSAGES ===\n');

    if (failedRequests.length > 0) {
      console.log('\n=== FAILED REQUESTS ===');
      failedRequests.forEach(f => console.log(`${f.status} - ${f.url}`));
      console.log('=== END FAILED REQUESTS ===\n');
    }

  } catch (e) {
    console.error('ERROR:', e.message);
  }

  await browser.close();
  console.log('\n=== TEST DONE ===');
}

simpleTest().catch(console.error);