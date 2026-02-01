const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

async function quickTest() {
  console.log('\n=== QUICK ERROR CHECK ===');
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ ignoreHTTPSErrors: true });
  const page = await context.newPage();
  const pageUrl = process.env.GAME_URL || 'http://localhost:8888/STARSECTOR_V6J_FINAL_WORKING.html';
  const outputDir = path.join(__dirname, 'test_output');
  fs.mkdirSync(outputDir, { recursive: true });

  let hasErrors = false;
  let errorCount = 0;
  const allErrors = [];

  page.on('console', msg => {
    const type = msg.type();
    const text = msg.text();
    
    if (type === 'error') {
      errorCount++;
      hasErrors = true;
      allErrors.push({ type: 'console', text, time: new Date().toISOString() });
      console.error(`[ERROR ${errorCount}] ${text}`);
    } else {
      console.log(`[${type}] ${text}`);
    }
  });

  page.on('pageerror', error => {
    errorCount++;
    hasErrors = true;
    allErrors.push({ type: 'page', text: error.message, time: new Date().toISOString() });
    console.error(`[PAGE ERROR ${errorCount}] ${error.message}`);
  });

  try {
    console.log('\n1. Loading page...');
    await page.goto(pageUrl, { waitUntil: 'domcontentloaded', timeout: 10000 });
    console.log('   ✅ Loaded\n');

    await page.waitForTimeout(2000);

    console.log('2. Clicking INITIALIZE...');
    const initResult = await page.evaluate(() => {
      const btn = document.getElementById('initButton');
      if (btn && !btn.disabled) {
        btn.click();
        return 'clicked';
      }
      return btn ? 'disabled' : 'not_found';
    });
    console.log(`   Result: ${initResult}\n`);

    if (initResult === 'clicked') {
      console.log('3. Waiting 20 seconds for init...');
      await page.waitForTimeout(20000);
      console.log('   ✅ Waited\n');

      const logs = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
      console.log('--- LOGS AFTER INIT ---');
      console.log(logs);
      console.log('--- END LOGS ---\n');

      const launchEnabled = await page.evaluate(() => {
        const btn = document.getElementById('launchButton');
        return btn ? !btn.disabled : false;
      });

      if (launchEnabled) {
        console.log('4. Clicking LAUNCH...');
        const launchResult = await page.evaluate(() => {
          const btn = document.getElementById('launchButton');
          if (btn && !btn.disabled) {
            btn.click();
            return 'clicked';
          }
          return 'disabled';
        });
        console.log(`   Result: ${launchResult}\n`);

        if (launchResult === 'clicked') {
          console.log('5. Waiting 20 seconds for launch...');
          await page.waitForTimeout(20000);
          console.log('   ✅ Waited\n');

          const finalLogs = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
          console.log('--- FINAL LOGS ---');
          console.log(finalLogs);
          console.log('--- END FINAL LOGS ---\n');
        }
      } else {
        console.log('4. Launch button NOT enabled - cannot test launch\n');
      }
    } else {
      console.log('⚠️ Could not click INITIALIZE button\n');
    }

  } catch (e) {
    console.error(`\nFATAL ERROR: ${e.message}`);
    errorCount++;
    hasErrors = true;
    allErrors.push({ type: 'fatal', text: e.message, time: new Date().toISOString() });
  }

  await page.screenshot({ path: path.join(outputDir, 'quick_test_screenshot.png'), fullPage: true });
  await browser.close();

  console.log('\n' + '='.repeat(60));
  console.log('SUMMARY');
  console.log('='.repeat(60));
  console.log(`Total Errors: ${errorCount}`);
  console.log(`Has Errors: ${hasErrors}`);
  
  if (allErrors.length > 0) {
    console.log('\nERROR DETAILS:');
    allErrors.forEach((err, i) => {
      console.log(`${i+1}. [${err.type.toUpperCase()}] ${err.text}`);
      console.log(`   Time: ${err.time}`);
    });
  } else {
    console.log('\n✅ NO ERRORS FOUND!');
  }
  
  console.log('='.repeat(60) + '\n');
  
  return { hasErrors, errorCount, allErrors };
}

quickTest().catch(console.error);
