const { chromium } = require('playwright');

(async () => {
    console.log('Starting verification...');
    const browser = await chromium.launch({
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    const context = await browser.newContext();
    const page = await context.newPage();

    let errorDetected = false;

    page.on('console', msg => {
        const text = msg.text();
        console.log(`PAGE LOG: ${text}`);
        
        if (!errorDetected && (
            text.includes('Fatal:') || 
            text.includes('NoSuchMethodError') || 
            text.includes('SecurityException') ||
            text.includes('UnsatisfiedLinkError') ||
            text.includes('NoClassDefFoundError'))) {
            console.error('!!! CRITICAL ERROR DETECTED. WAITING FOR TRACE !!!');
            errorDetected = true;
            // Delay exit to allow trace to print
            setTimeout(() => {
                console.error('!!! EXITING NOW !!!');
                process.exit(1);
            }, 15000);
        }
    });

    try {
        await page.goto('http://localhost:8888/launch.html');
        console.log('Navigating to http://localhost:8888/launch.html...');

        await page.waitForSelector('#startBtn');
        console.log('Clicking Launch...');
        await page.click('#startBtn');

        console.log('Waiting for canvas or error...');
        await page.waitForSelector('canvas', { state: 'visible', timeout: 300000 });
        
        console.log('Canvas detected! Game initialized.');
        await page.waitForTimeout(10000);
        
    } catch (err) {
        if (!errorDetected) {
            console.error(err);
        } else {
            // Error already handled by console listener
            await new Promise(resolve => setTimeout(resolve, 20000));
        }
    } finally {
        console.log('Closing browser...');
        await browser.close();
    }
})();
