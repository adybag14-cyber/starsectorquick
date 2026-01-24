const { chromium } = require('playwright');
const fs = require('fs');

async function finalTest() {
  console.log('\n' + '='.repeat(80));
  console.log('🎮 STARSECTOR 2 - FINAL AUTOMATED TEST');
  console.log('='.repeat(80));

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  let allLogs = [];
  let allErrors = [];

  page.on('console', msg => {
    const text = msg.text();
    const type = msg.type();
    allLogs.push({ type, text });
    console.log(`[${type}] ${text}`);
  });

  page.on('pageerror', err => {
    allErrors.push(err.message);
    console.error('[PAGE ERROR]', err.message);
  });

  page.on('requestfailed', request => {
    console.error(`[REQUEST FAILED] ${request.url()} - ${request.failure().errorText}`);
  });

  try {
    console.log('\n📄 Step 1: Loading page...');
    await page.goto('http://localhost:8080/STARSECTOR_V6J_FINAL_WORKING.html', { waitUntil: 'domcontentloaded', timeout: 15000 });
    console.log('✅ Page loaded successfully!\n');

    await page.waitForTimeout(2000);

    // Get initial logs
    const initialLogs = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
    console.log('--- INITIAL PAGE LOGS ---');
    console.log(initialLogs);
    console.log('--- END INITIAL LOGS ---\n');

    console.log('🧪 Step 2: Clicking INITIALIZE button...');
    // Use evaluate to click to avoid hanging
    const initResult = await page.evaluate(() => {
      const btn = document.getElementById('initButton');
      if (btn && !btn.disabled) {
        btn.click();
        return { success: true, message: 'Clicked successfully' };
      }
      return { success: false, message: btn ? 'Button is disabled' : 'Button not found' };
    });
    console.log(`Result: ${initResult.success ? '✅ Clicked!' : '❌ ' + initResult.message}\n`);

    if (initResult.success) {
      console.log('⏳ Step 3: Waiting for initialization (30s)...');
      await page.waitForTimeout(30000);
      console.log('✅ Wait complete!\n');

      // Get logs after initialization
      const afterInitLogs = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
      console.log('--- LOGS AFTER INITIALIZATION ---');
      console.log(afterInitLogs);
      console.log('--- END LOGS ---\n');

      // Check if launch button is enabled
      const launchEnabled = await page.evaluate(() => {
        const btn = document.getElementById('launchButton');
        return btn ? !btn.disabled : false;
      });

      console.log(`🚀 Step 4: Launch button enabled: ${launchEnabled}\n`);

      if (launchEnabled) {
        console.log('🚀 Step 5: Clicking LAUNCH button...');
        const launchResult = await page.evaluate(() => {
          const btn = document.getElementById('launchButton');
          if (btn && !btn.disabled) {
            btn.click();
            return { success: true };
          }
          return { success: false };
        });
        console.log(launchResult.success ? '✅ Clicked!' : '❌ Failed to click');

        console.log('\n⏳ Step 6: Waiting for game to launch (30s)...');
        await page.waitForTimeout(30000);
        console.log('✅ Wait complete!\n');

        // Get final logs
        const finalLogs = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
        console.log('--- FINAL LOGS ---');
        console.log(finalLogs);
        console.log('--- END FINAL LOGS ---\n');

        // Screenshot
        await page.screenshot({ path: 'starsector_final_screenshot.png', fullPage: true });
        console.log('📸 Screenshot saved: starsector_final_screenshot.png\n');
      } else {
        console.log('❌ Cannot launch - button is still disabled');
      }
    } else {
      console.log('❌ Cannot initialize - button error');
    }

  } catch (error) {
    console.error('\n❌ TEST FAILED:', error.message);
    console.error(error.stack);
    allErrors.push(error.message);
  }

  // Summary
  console.log('\n' + '='.repeat(80));
  console.log('📊 TEST SUMMARY');
  console.log('='.repeat(80));
  console.log(`Total console logs: ${allLogs.length}`);
  console.log(`Total errors: ${allErrors.length}`);

  if (allErrors.length > 0) {
    console.log('\n⚠️ ERRORS:');
    allErrors.forEach((err, i) => console.log(`${i+1}. ${err}`));
  }

  // Save logs to file
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const logFile = `starsector_test_${timestamp}.log`;
  fs.writeFileSync(logFile, JSON.stringify({
    allLogs,
    allErrors,
    finalLogs: await page.$eval('#log', el => el.innerText).catch(() => 'No logs')
  }, null, 2));
  console.log(`\n📝 Logs saved to: ${logFile}`);

  await browser.close();
  console.log('\n' + '='.repeat(80));
  console.log('✅ TEST COMPLETE');
  console.log('='.repeat(80) + '\n');
}

finalTest().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});