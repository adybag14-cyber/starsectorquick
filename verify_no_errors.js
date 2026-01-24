const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUTPUT_DIR = './test_output';
if (!fs.existsSync(OUTPUT_DIR)) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
}

async function verifyNoErrors() {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
  const logFile = path.join(OUTPUT_DIR, `test_${timestamp}.log`);
  
  console.log('\n' + '='.repeat(80));
  console.log('🎮 STARSECTOR 2 - COMPREHENSIVE ERROR VERIFICATION');
  console.log('='.repeat(80));
  console.log(`Log file: ${logFile}`);
  console.log(`Started: ${new Date().toISOString()}\n`);

  const browser = await chromium.launch({ 
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox']
  });
  
  const context = await browser.newContext({
    viewport: { width: 1280, height: 720 }
  });
  
  const page = await context.newPage();
  
  // Comprehensive logging
  const testLog = {
    startTime: new Date().toISOString(),
    consoleLogs: [],
    errors: [],
    warnings: [],
    pageErrors: [],
    networkErrors: [],
    steps: []
  };

  // Console handler
  page.on('console', msg => {
    const entry = {
      timestamp: new Date().toISOString(),
      type: msg.type(),
      text: msg.text(),
      location: msg.location()
    };
    testLog.consoleLogs.push(entry);
    
    if (msg.type() === 'error') {
      testLog.errors.push(entry);
      console.error(`[CONSOLE ERROR] ${msg.text()}`);
    } else if (msg.type() === 'warning') {
      testLog.warnings.push(entry);
      console.warn(`[CONSOLE WARN] ${msg.text()}`);
    } else {
      console.log(`[CONSOLE ${msg.type()}] ${msg.text()}`);
    }
  });

  // Page error handler (JavaScript errors)
  page.on('pageerror', error => {
    const entry = {
      timestamp: new Date().toISOString(),
      message: error.message,
      stack: error.stack
    };
    testLog.pageErrors.push(entry);
    console.error('[PAGE ERROR]', error.message);
  });

  // Network error handler
  page.on('requestfailed', request => {
    const failure = request.failure();
    const entry = {
      timestamp: new Date().toISOString(),
      url: request.url(),
      errorText: failure ? failure.errorText : 'Unknown'
    };
    testLog.networkErrors.push(entry);
    console.error(`[NETWORK ERROR] ${request.url()} - ${entry.errorText}`);
  });

  // Response handler for status codes
  page.on('response', response => {
    const status = response.status();
    if (status >= 400) {
      const entry = {
        timestamp: new Date().toISOString(),
        url: response.url(),
        status: status,
        statusText: response.statusText()
      };
      testLog.networkErrors.push(entry);
      console.error(`[HTTP ${status}] ${response.url()}`);
    }
  });

  try {
    // STEP 1: Load page
    const step1 = {
      step: 1,
      name: 'Load Page',
      startTime: new Date().toISOString()
    };
    
    console.log('\n📄 STEP 1: Loading page...');
    try {
      await page.goto('http://localhost:8080/STARSECTOR_V6J_FINAL_WORKING.html', 
        { waitUntil: 'domcontentloaded', timeout: 15000 });
      step1.success = true;
      step1.endTime = new Date().toISOString();
      step1.duration = (new Date(step1.endTime) - new Date(step1.startTime)) + 'ms';
      console.log('   ✅ Page loaded');
    } catch (e) {
      step1.success = false;
      step1.error = e.message;
      console.error('   ❌ Failed to load page:', e.message);
    }
    testLog.steps.push(step1);
    await page.screenshot({ path: path.join(OUTPUT_DIR, 'step1_loaded.png') });
    await page.waitForTimeout(2000);

    // Get initial logs from page
    const initialPageLogs = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
    console.log('\n--- INITIAL PAGE LOGS ---');
    console.log(initialPageLogs);
    console.log('--- END INITIAL LOGS ---\n');
    testLog.initialPageLogs = initialPageLogs;

    // STEP 2: Check buttons
    const step2 = {
      step: 2,
      name: 'Check Buttons',
      startTime: new Date().toISOString()
    };
    
    console.log('🔍 STEP 2: Checking button elements...');
    try {
      const initExists = await page.$('#initButton') !== null;
      const launchExists = await page.$('#launchButton') !== null;
      const initDisabled = await page.$eval('#initButton', el => el.disabled).catch(() => true);
      const launchDisabled = await page.$eval('#launchButton', el => el.disabled).catch(() => true);
      
      step2.buttons = {
        init: { exists: initExists, disabled: initDisabled },
        launch: { exists: launchExists, disabled: launchDisabled }
      };
      step2.success = true;
      
      console.log(`   INITIALIZE: ${initExists ? '✅ Found' : '❌ Not found'}, ${initDisabled ? 'Disabled' : 'Enabled'}`);
      console.log(`   LAUNCH: ${launchExists ? '✅ Found' : '❌ Not found'}, ${launchDisabled ? 'Disabled' : 'Enabled'}`);
    } catch (e) {
      step2.success = false;
      step2.error = e.message;
      console.error('   ❌ Button check failed:', e.message);
    }
    step2.endTime = new Date().toISOString();
    testLog.steps.push(step2);

    // STEP 3: Click INITIALIZE
    const step3 = {
      step: 3,
      name: 'Click INITIALIZE',
      startTime: new Date().toISOString()
    };
    
    console.log('\n🧪 STEP 3: Clicking INITIALIZE button...');
    try {
      const clickResult = await page.evaluate(() => {
        const btn = document.getElementById('initButton');
        if (btn && !btn.disabled) {
          btn.click();
          return { success: true, message: 'Clicked successfully' };
        }
        return btn ? 
          { success: false, message: 'Button is disabled' } : 
          { success: false, message: 'Button not found' };
      });
      
      step3.clickResult = clickResult;
      step3.success = clickResult.success;
      console.log(`   ${clickResult.success ? '✅' : '❌'} ${clickResult.message}`);
    } catch (e) {
      step3.success = false;
      step3.error = e.message;
      console.error('   ❌ Click failed:', e.message);
    }
    step3.endTime = new Date().toISOString();
    step3.duration = (new Date(step3.endTime) - new Date(step3.startTime)) + 'ms';
    testLog.steps.push(step3);
    await page.screenshot({ path: path.join(OUTPUT_DIR, 'step3_initialize_clicked.png') });

    // STEP 4: Wait for initialization
    const step4 = {
      step: 4,
      name: 'Wait for Initialization',
      startTime: new Date().toISOString()
    };
    
    console.log('\n⏳ STEP 4: Waiting for initialization (30s)...');
    await page.waitForTimeout(30000);
    step4.endTime = new Date().toISOString();
    step4.duration = '30000ms';
    step4.success = true;
    testLog.steps.push(step4);
    
    // Get logs after initialization
    const afterInitLogs = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
    console.log('\n--- LOGS AFTER INITIALIZATION ---');
    console.log(afterInitLogs);
    console.log('--- END LOGS ---\n');
    testLog.afterInitLogs = afterInitLogs;
    await page.screenshot({ path: path.join(OUTPUT_DIR, 'step4_after_init.png') });

    // Check if launch is enabled
    const launchEnabled = await page.evaluate(() => {
      const btn = document.getElementById('launchButton');
      return btn ? !btn.disabled : false;
    });
    console.log(`\n🎯 Launch button enabled: ${launchEnabled}`);
    testLog.launchEnabled = launchEnabled;

    // STEP 5: Click LAUNCH (if enabled)
    if (launchEnabled) {
      const step5 = {
        step: 5,
        name: 'Click LAUNCH',
        startTime: new Date().toISOString()
      };
      
      console.log('\n🚀 STEP 5: Clicking LAUNCH button...');
      try {
        const clickResult = await page.evaluate(() => {
          const btn = document.getElementById('launchButton');
          if (btn && !btn.disabled) {
            btn.click();
            return { success: true };
          }
          return { success: false, message: 'Button not available' };
        });
        
        step5.clickResult = clickResult;
        step5.success = clickResult.success;
        console.log(`   ${clickResult.success ? '✅' : '❌'} ${clickResult.message || 'Clicked'}`);
      } catch (e) {
        step5.success = false;
        step5.error = e.message;
        console.error('   ❌ Click failed:', e.message);
      }
      step5.endTime = new Date().toISOString();
      step5.duration = (new Date(step5.endTime) - new Date(step5.startTime)) + 'ms';
      testLog.steps.push(step5);
      await page.screenshot({ path: path.join(OUTPUT_DIR, 'step5_launch_clicked.png') });

      // STEP 6: Wait for game launch
      const step6 = {
        step: 6,
        name: 'Wait for Game Launch',
        startTime: new Date().toISOString()
      };
      
      console.log('\n⏳ STEP 6: Waiting for game to launch (30s)...');
      await page.waitForTimeout(30000);
      step6.endTime = new Date().toISOString();
      step6.duration = '30000ms';
      step6.success = true;
      testLog.steps.push(step6);
      
      // Get final logs
      const finalLogs = await page.$eval('#log', el => el.innerText).catch(() => 'No logs');
      console.log('\n--- FINAL LOGS ---');
      console.log(finalLogs);
      console.log('--- END FINAL LOGS ---\n');
      testLog.finalLogs = finalLogs;
      await page.screenshot({ path: path.join(OUTPUT_DIR, 'step6_final.png') });
    } else {
      console.log('\n⚠️ SKIPPING LAUNCH - Button not enabled');
      testLog.steps.push({
        step: 5,
        name: 'Launch Skipped',
        startTime: new Date().toISOString(),
        endTime: new Date().toISOString(),
        success: false,
        reason: 'Launch button was disabled'
      });
    }

  } catch (error) {
    console.error('\n❌ TEST ERROR:', error.message);
    console.error(error.stack);
    testLog.fatalError = {
      message: error.message,
      stack: error.stack,
      timestamp: new Date().toISOString()
    };
  }

  // Finalize log
  testLog.endTime = new Date().toISOString();
  testLog.totalDuration = (new Date(testLog.endTime) - new Date(testLog.startTime)) + 'ms';
  testLog.summary = {
    totalConsoleLogs: testLog.consoleLogs.length,
    totalErrors: testLog.errors.length + testLog.pageErrors.length,
    consoleErrors: testLog.errors.length,
    pageErrors: testLog.pageErrors.length,
    warnings: testLog.warnings.length,
    networkErrors: testLog.networkErrors.length,
    stepsCompleted: testLog.steps.filter(s => s.success).length,
    stepsTotal: testLog.steps.length
  };

  // Print summary
  console.log('\n' + '='.repeat(80));
  console.log('📊 TEST SUMMARY');
  console.log('='.repeat(80));
  console.log(`Total Duration: ${testLog.totalDuration}`);
  console.log(`Total Console Logs: ${testLog.summary.totalConsoleLogs}`);
  console.log(`Total Errors: ${testLog.summary.totalErrors}`);
  console.log(`  - Console Errors: ${testLog.summary.consoleErrors}`);
  console.log(`  - Page Errors: ${testLog.summary.pageErrors}`);
  console.log(`Warnings: ${testLog.summary.warnings}`);
  console.log(`Network Errors: ${testLog.summary.networkErrors}`);
  console.log(`Steps Completed: ${testLog.summary.stepsCompleted}/${testLog.summary.stepsTotal}`);
  
  if (testLog.summary.totalErrors > 0) {
    console.log('\n⚠️ ERRORS FOUND:');
    testLog.errors.forEach((err, i) => {
      console.log(`\n${i+1}. [CONSOLE ERROR] ${err.text}`);
      console.log(`   Time: ${err.timestamp}`);
      if (err.location) {
        console.log(`   Location: ${err.location.url}:${err.location.lineNumber}`);
      }
    });
    testLog.pageErrors.forEach((err, i) => {
      console.log(`\n${i+1}. [PAGE ERROR] ${err.message}`);
      console.log(`   Time: ${err.timestamp}`);
    });
  }
  
  if (testLog.summary.networkErrors > 0) {
    console.log('\n⚠️ NETWORK ERRORS:');
    testLog.networkErrors.forEach((err, i) => {
      console.log(`${i+1}. ${err.url || err.status}
  - ${err.errorText || err.statusText}`);
    });
  }
  
  if (testLog.summary.totalErrors === 0 && testLog.summary.networkErrors === 0) {
    console.log('\n✅ NO ERRORS FOUND!');
  }
  
  // Save detailed log
  fs.writeFileSync(logFile, JSON.stringify(testLog, null, 2));
  console.log(`\n📝 Detailed log saved to: ${logFile}`);
  console.log(`📸 Screenshots saved to: ${OUTPUT_DIR}`);
  
  await browser.close();
  
  console.log('\n' + '='.repeat(80));
  console.log('✅ VERIFICATION COMPLETE');
  console.log('='.repeat(80) + '\n');
  
  return testLog;
}

verifyNoErrors().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});