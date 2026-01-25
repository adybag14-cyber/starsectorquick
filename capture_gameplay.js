const { chromium } = require('playwright');
const path = require('path');

async function captureGameplay() {
    console.log('🧪 Starting Native Transition Capture...');
    
    const browser = await chromium.launch({
        executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
        headless: true
    });
    
    const context = await browser.newContext({
        recordVideo: {
            dir: path.join(__dirname, 'test_output', 'videos'),
            size: { width: 1280, height: 800 }
        }
    });
    
    const page = await context.newPage();

    try {
        console.log('🔗 Navigating to noVNC...');
        await page.goto('http://144.21.61.111:8000', { waitUntil: 'networkidle', timeout: 60000 });
        
        await page.waitForTimeout(5000);
        
        const connectButton = await page.$('input[type="button"][value="Connect"]');
        if (connectButton) {
            await connectButton.click();
            console.log('🖱️ Connected to VNC.');
            await page.waitForTimeout(10000);
        }

        console.log('📸 Screenshot of Launcher before clicking...');
        await page.screenshot({ path: path.join(__dirname, 'test_output', 'native_pre_click.png') });

        console.log('🖱️ Clicking Play button area (multiple points)...');
        // The Play button is centered at the bottom of the launcher (597x373 window centered in 1280x800)
        // Launcher is at (341, 213). Bottom center relative to launcher is (298, 323).
        // Absolute: (341+298, 213+323) = (639, 536)
        const clickPoints = [
            {x: 639, y: 536},
            {x: 639, y: 540},
            {x: 639, y: 550},
            {x: 639, y: 530}
        ];

        for (const pt of clickPoints) {
            await page.mouse.click(pt.x, pt.y);
            await page.waitForTimeout(500);
        }
        
        console.log('⌨️ Sending Enter...');
        await page.keyboard.press('Enter');

        console.log('🎬 Recording transition for 1 minute...');
        for (let i = 0; i < 6; i++) {
            await page.screenshot({ path: path.join(__dirname, 'test_output', `native_transition_${i}.png`) });
            await page.waitForTimeout(10000);
            console.log(`📸 Progress screenshot ${i}...`);
        }

    } catch (error) {
        console.error('❌ Error:', error.message);
    } finally {
        await context.close();
        await browser.close();
        console.log('🏁 Finished.');
    }
}

captureGameplay();