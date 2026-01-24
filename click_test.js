const { chromium } = require('playwright');

async function clickTest() {
  console.log('\n=== CLICK TEST ===');
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  let allMessages = [];
  let allErrors = [];

  page.on('console', msg => {
    const text = msg.text();
    allMessages.push({ type: msg.type(), text });
    console.log(`[${msg.type()}] ${text}`);
  });

  page.on('pageerror', err => {
    allErrors.push(err.message);
    console.error('[ERROR]', err.message);
  });

  try {
    console.log('\n1. Loading page...');
    await page.goto('http://localhost:8080/STARSECTOR_V6J_FINAL_WORKING.html', { waitUntil: 'domcontentloaded', timeout: 15000 });
    console.log('   ✅ Page loaded');

    await page.waitForTimeout(1000);

    console.log('\n2. Clicking INITIALIZE...');
    await page.click('#initButton', { timeout: 5000 });
    console.log('   ✅ Clicked');

    console.log('\n3. Waiting 10 seconds for initialization...');
    await page.waitForTimeout(10000);
    console.log('   ✅ Waited');

    console.log('\n4. Checking log output...');
    const logText = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
    console.log('\n--- LOGS AFTER INITIALIZE ---');
    console.log(logText);
    console.log('--- END LOGS ---\n');

    console.log('\n5. Checking if launch button enabled...');
    const launchEnabled = await page.$eval('#launchButton', el => !el.disabled).catch(() => false);
    console.log(`   Launch button enabled: ${launchEnabled}`);

    if (launchEnabled) {
      console.log('\n6. Clicking LAUNCH...');
      await page.click('#launchButton', { timeout: 5000 });
      console.log('   ✅ Clicked');

      console.log('\n7. Waiting 15 seconds for launch...');
      await page.waitForTimeout(15000);
      console.log('   ✅ Waited');

      console.log('\n8. Getting final logs...');
      const finalLogText = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
      console.log('\n--- FINAL LOGS ---');
      console.log(finalLogText);
      console.log('--- END FINAL LOGS ---\n');
    } else {
      console.log('\n   Launch button not enabled, cannot launch');
    }

    await page.screenshot({ path: 'click_test_screenshot.png', fullPage: true });
    console.log('\n   📸 Screenshot saved to click_test_screenshot.png');

  } catch (e) {
    console.error('\n   ❌ ERROR:', e.message);
    allErrors.push(e.message);
  }

  await browser.close();

  console.log('\n=== TEST SUMMARY ===');
  console.log(`Total messages: ${allMessages.length}`);
  console.log(`Total errors: ${allErrors.length}`);

  if (allErrors.length > 0) {
    console.log('\nErrors:');
    allErrors.forEach((err, i) => console.log(`${i+1}. ${err}`));
  }

  console.log('\n=== DONE ===\n');
}

clickTest().catch(console.error);