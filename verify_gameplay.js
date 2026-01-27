const { chromium } = require('playwright');
const path = require('path');

(async () => {
  console.log('Starting verification...');
  const browser = await chromium.launch({ headless: false }); // Headless: false to see it action
  const page = await browser.newPage();
  
  // Capture console logs
  page.on('console', msg => console.log('PAGE LOG:', msg.text()));
  page.on('pageerror', exception => console.log('PAGE ERROR:', exception));

  // Use local server for faster feedback loop
  const url = 'http://localhost:8888/launch.html'; 
  console.log(`Navigating to ${url}...`);
  
  try {
    await page.goto(url);
    await page.waitForTimeout(2000);
    await page.screenshot({ path: 'step1_loaded.png' });

    console.log('Clicking Launch...');
    await page.click('#startBtn');
    
    // Wait for the canvas to be created by CheerpJ
    console.log('Waiting for canvas...');
    await page.waitForSelector('canvas', { timeout: 60000 });
    await page.screenshot({ path: 'step2_canvas_ready.png' });

    console.log('Waiting for game load (120s)...');
    // Allow time for JVM init and game load
    await page.waitForTimeout(120000); 
    await page.screenshot({ path: 'step3_game_loaded.png' });

    // Try to click "Play" (Assuming typical coordinates for 1024x768)
    // Starsector launcher usually has a big "Launch" button or similar. 
    // If it skipped the launcher (Direct Launch), it might be at the main menu.
    // Main Menu: "Campaign", "Missions", "Tutorial", "Exit"
    // Let's assume we are at the main menu.
    
    const canvas = await page.$('canvas');
    const box = await canvas.boundingBox();
    
    if (box) {
        console.log('Canvas found at:', box);
        
        // Simulating clicks on potential menu items
        // Coordinates need to be guessed or calibrated.
        // Let's try clicking the center first to focus?
        await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
        
        // Take a screenshot after interaction
        await page.waitForTimeout(5000);
        await page.screenshot({ path: 'step4_after_interaction.png' });
    }

  } catch (e) {
    console.error('Error:', e);
    await page.screenshot({ path: 'error_snapshot.png' });
  } finally {
    console.log('Closing browser...');
    await browser.close();
  }
})();
