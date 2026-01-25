const { chromium } = require('playwright');
const path = require('path');

async function captureGameplay() {
    console.log('🧪 Starting Native Stream Debug...');
    
    const browser = await chromium.launch({
        executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
        headless: true
    });
    
    const page = await browser.newPage();
    
    page.on('console', msg => console.log('PAGE LOG:', msg.text()));
    page.on('pageerror', err => console.log('PAGE ERROR:', err.message));

    try {
        console.log('🔗 Navigating to http://144.21.61.111:8000 ...');
        await page.goto('http://144.21.61.111:8000', { waitUntil: 'networkidle', timeout: 60000 });
        
        await page.waitForTimeout(5000);
        
        const connectButton = await page.$('input[type="button"][value="Connect"]');
        if (connectButton) {
            console.log('🖱️ Connect button FOUND. Clicking...');
            await connectButton.click();
            await page.waitForTimeout(5000);
        } else {
            console.log('❌ Connect button NOT found.');
            const body = await page.innerHTML('body');
            console.log('📄 Body snippet:', body.substring(0, 500));
        }

        await page.screenshot({ path: path.join(__dirname, 'test_output', 'final_debug.png') });

    } catch (error) {
        console.error('❌ Error:', error.message);
    } finally {
        await browser.close();
        console.log('🏁 Browser closed.');
    }
}

captureGameplay();
