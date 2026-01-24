const { chromium } = require('playwright');

async function buttonCheck() {
  console.log('\n=== BUTTON CHECK ===');
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  page.on('console', msg => console.log(`[${msg.type()}] ${msg.text()}`));
  page.on('pageerror', err => console.error('[ERROR]', err.message));

  try {
    console.log('Loading page...');
    await page.goto('http://localhost:8080/STARSECTOR_V6J_FINAL_WORKING.html', { waitUntil: 'domcontentloaded', timeout: 10000 });
    console.log('Page loaded!\n');

    await page.waitForTimeout(2000);

    // Check button elements
    console.log('Checking button elements...');
    
    const initButton = await page.$('#initButton');
    console.log('INITIALIZE button found:', initButton !== null);
    
    const launchButton = await page.$('#launchButton');
    console.log('LAUNCH button found:', launchButton !== null);

    // Get button properties
    if (initButton) {
      const initText = await page.$eval('#initButton', el => el.textContent);
      const initDisabled = await page.$eval('#initButton', el => el.disabled);
      console.log('\nINITIALIZE button:');
      console.log('  Text:', initText);
      console.log('  Disabled:', initDisabled);
    }

    if (launchButton) {
      const launchText = await page.$eval('#launchButton', el => el.textContent);
      const launchDisabled = await page.$eval('#launchButton', el => el.disabled);
      console.log('\nLAUNCH button:');
      console.log('  Text:', launchText);
      console.log('  Disabled:', launchDisabled);
    }

    // Get current logs
    const logText = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
    console.log('\n--- CURRENT LOGS ---');
    console.log(logText);
    console.log('--- END LOGS ---\n');

    await page.screenshot({ path: 'button_check_screenshot.png' });
    console.log('Screenshot saved!');

  } catch (e) {
    console.error('ERROR:', e.message);
  }

  await browser.close();
  console.log('\n=== CHECK DONE ===');
}

buttonCheck().catch(console.error);