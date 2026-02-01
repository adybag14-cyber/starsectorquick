const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

async function runGameCheck() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ ignoreHTTPSErrors: true });
  const page = await context.newPage();

  page.on('console', msg => {
    console.log(`[CONSOLE ${msg.type()}] ${msg.text()}`);
  });
  page.on('pageerror', error => {
    console.error(`[PAGE ERROR] ${error.message}`);
  });

  const url = process.env.GAME_URL || 'http://localhost:8888/STARSECTOR_V6J_FINAL_WORKING.html';
  const maxAttempts = Number.parseInt(process.env.PW_ATTEMPTS || '3', 10);
  const outputDir = path.join(process.cwd(), 'test_output');
  fs.mkdirSync(outputDir, { recursive: true });

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    console.log(`Loading ${url} (attempt ${attempt}/${maxAttempts})...`);
    await page.goto(url, { waitUntil: 'domcontentloaded' });

    await page.waitForTimeout(2000);

    if (await page.$('#initButton')) {
      console.log('Clicking initialize button...');
      await page.click('#initButton');
      await page.waitForTimeout(15000);

      const launchEnabled = await page.$eval('#launchButton', el => !el.disabled).catch(() => false);
      console.log(`Launch enabled: ${launchEnabled}`);
      if (launchEnabled) {
        console.log('Clicking launch button...');
        await page.click('#launchButton');
        await page.waitForTimeout(20000);
      }
    } else if (await page.$('#startBtn')) {
      console.log('Clicking launch button...');
      await page.click('#startBtn');
      await page.waitForTimeout(15000);
    } else {
      console.log('No launch controls found.');
    }

    const logText = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
    console.log('\n--- LOG OUTPUT ---');
    console.log(logText);

    const screenshotPath = path.join(outputDir, `playwright_game_attempt_${attempt}.png`);
    await page.screenshot({ path: screenshotPath, fullPage: true });
    console.log(`Screenshot saved to ${screenshotPath}`);

    if (
      !logText.includes('Missing') &&
      !logText.includes('Init failed') &&
      !logText.includes('Could not find or load main class')
    ) {
      console.log('No missing JARs detected; stopping retries.');
      break;
    }

    if (attempt < maxAttempts) {
      console.log('Missing assets detected; retrying after delay...');
      await page.waitForTimeout(3000);
    }
  }

  await browser.close();
}

runGameCheck().catch(error => {
  console.error('Playwright run failed:', error);
  process.exit(1);
});
