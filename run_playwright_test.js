const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

async function runTest() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ ignoreHTTPSErrors: true });
  const page = await context.newPage();
  const pageUrl = process.env.GAME_URL || 'http://localhost:8888/STARSECTOR_V6J_FINAL_WORKING.html';
  const outputDir = path.join(__dirname, 'test_output');
  fs.mkdirSync(outputDir, { recursive: true });

  // Capture console
  page.on('console', msg =>console.log(`[CONSOLE ${msg.type()}] ${msg.text()}`));
  page.on('pageerror', error => console.error(`[PAGE ERROR] ${error.message}`));

  console.log(`Loading ${pageUrl}...`);
  await page.goto(pageUrl, { waitUntil: 'domcontentloaded' });

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

  const screenshotPath = path.join(outputDir, 'test_screenshot.png');
  await page.screenshot({ path: screenshotPath, fullPage: true });
  console.log(`\nScreenshot saved to ${screenshotPath}`);
  
  await browser.close();
  console.log('\n=== TEST COMPLETE ===\n');
}

runTest().catch(console.error);
