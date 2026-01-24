const { chromium } = require('playwright');
const fs = require('fs');

async function runTest() {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  // Capture console
  page.on('console', msg =>console.log(`[CONSOLE ${msg.type()}] ${msg.text()}`));
  page.on('pageerror', error => console.error(`[PAGE ERROR] ${error.message}`));

  console.log('Loading http://localhost:8080/test_fixed.html...');
  await page.goto('http://localhost:8080/test_fixed.html', {waitUntil: 'domcontentloaded'});

  await page.waitForTimeout(3000);

  console.log('\n--- INITIAL LOGS ---');
  const initialLogs = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
  console.log(initialLogs);

  console.log('\nClicking INITIALIZE...');
  await page.click('#initButton');
  await page.waitForTimeout(20000);

  console.log('\n--- AFTER INIT LOGS ---');
  const afterInitLogs = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
  console.log(afterInitLogs);

  // Wait a bit for button to enable
  await page.waitForTimeout(2000);
  
  const canLaunch = await page.$eval('#launchButton', el => !el.disabled).catch(() => false);
  if (canLaunch) {
    console.log('Clicking LAUNCH...');
    await page.click('#launchButton');
    await page.waitForTimeout(20000);

    console.log('\n--- FINAL LOGS ---');
    const finalLogs = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
    console.log(finalLogs);
  } else {
    console.log('\nLaunch button is disabled!');
  }

  await page.screenshot({ path: 'test_screenshot.png' });
  console.log('\nScreenshot saved to test_screenshot.png');
  
  await browser.close();
  console.log('\n=== TEST COMPLETE ===\n');
}

runTest().catch(console.error);