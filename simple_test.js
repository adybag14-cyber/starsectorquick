const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

async function simpleTest() {
  console.log('\n=== SIMPLE PLAYWRIGHT TEST ===');
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ ignoreHTTPSErrors: true });
  const page = await context.newPage();
  const pageUrl = process.env.GAME_URL || 'http://localhost:8888/STARSECTOR_V6J_FINAL_WORKING.html';
  const outputDir = path.join(__dirname, 'test_output');
  fs.mkdirSync(outputDir, { recursive: true });

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
    await page.goto(pageUrl, { waitUntil: 'domcontentloaded', timeout: 10000 });
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

  await page.screenshot({ path: path.join(outputDir, 'simple_test_screenshot.png'), fullPage: true });
  await browser.close();
  console.log('\n=== TEST DONE ===');
}

simpleTest().catch(console.error);
