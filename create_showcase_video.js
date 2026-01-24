const { chromium } = require('playwright');
const path = require('path');

async function createShowcaseVideo() {
    console.log('🎬 Creating Professional Showcase Video...');
    
    const browser = await chromium.launch({
        headless: false,
        args: [
            '--start-maximized',
            '--disable-blink-features=AutomationControlled'
        ]
    });
    
    const context = await browser.newContext({
        viewport: { width: 1920, height: 1080 },
        recordVideo: {
            dir: './videos',
            size: { width: 1920, height: 1080 }
        }
    });
    
    const page = await context.newPage();
    
    try {
        // Scene 1: Homepage Introduction
        console.log('📺 Scene 1: Homepage Introduction');
        await page.goto('file://' + path.resolve(__dirname, 'index.html'));
        await page.waitForTimeout(2000);
        
        // Smooth scroll to overview
        await page.evaluate(() => {
            document.querySelector('#overview').scrollIntoView({ behavior: 'smooth' });
        });
        await page.waitForTimeout(1500);
        
        // Scene 2: Features Section
        console.log('📺 Scene 2: Features Section');
        await page.evaluate(() => {
            document.querySelector('#features').scrollIntoView({ behavior: 'smooth' });
        });
        await page.waitForTimeout(1000);
        
        // Hover over feature cards
        const featureCards = await page.$$('.feature-card');
        for (let i = 0; i < Math.min(3, featureCards.length); i++) {
            await featureCards[i].hover();
            await page.waitForTimeout(800);
        }
        
        // Scene 3: Technology Stack
        console.log('📺 Scene 3: Technology Stack');
        const techStack = await page.$('#tech-stack');
        if (techStack) {
            await techStack.scrollIntoViewIfNeeded();
            await page.waitForTimeout(1500);
        }
        
        // Scene 4: Play Now Section
        console.log('📺 Scene 4: Play Now Section');
        const playNow = await page.$('#play-now');
        if (playNow) {
            await playNow.scrollIntoViewIfNeeded();
            await page.waitForTimeout(1000);
        }
        
        // Click launch button
        const launchBtn = await page.$('#launchBtn');
        if (launchBtn) {
            await launchBtn.click();
            await page.waitForTimeout(2000);
        }
        
        // Scene 5: Documentation Section
        console.log('📺 Scene 5: Documentation Section');
        const docs = await page.$('#documentation');
        if (docs) {
            await docs.scrollIntoViewIfNeeded();
            await page.waitForTimeout(1500);
        }
        
        // Scene 6: Resources Section
        console.log('📺 Scene 6: Resources Section');
        const resources = await page.$('#resources');
        if (resources) {
            await resources.scrollIntoViewIfNeeded();
            await page.waitForTimeout(1500);
        }
        
        // Scene 7: Game Launcher
        console.log('📺 Scene 7: Game Launcher');
        await page.goto('file://' + path.resolve(__dirname, 'STARSECTOR_V6J_FINAL_WORKING.html'));
        await page.waitForTimeout(2000);
        
        // Click initialize button
        const initBtn = await page.$('#initButton');
        if (initBtn) {
            await initBtn.click();
            await page.waitForTimeout(3000);
        }
        
        // Click launch button
        const gameLaunchBtn = await page.$('#launchButton');
        if (gameLaunchBtn) {
            await gameLaunchBtn.click();
            await page.waitForTimeout(4000);
        }
        
        // Scene 8: Final Shot
        console.log('📺 Scene 8: Final Shot');
        await page.evaluate(() => {
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
        await page.waitForTimeout(2000);
        
        console.log('✅ Video recording complete!');
        
    } catch (error) {
        console.error('❌ Error creating video:', error);
    } finally {
        await context.close();
        await browser.close();
    }
}

// Run the showcase
createShowcaseVideo().then(() => {
    console.log('🎉 Showcase video created successfully!');
    console.log('📁 Video saved in ./videos directory');
}).catch(error => {
    console.error('❌ Failed to create showcase video:', error);
    process.exit(1);
});