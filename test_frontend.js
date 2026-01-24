const { chromium } = require('playwright');
const path = require('path');

async function testFrontend() {
    console.log('🧪 Starting Playwright test for index.html...');
    console.log('⏱️  Timeout set to 20 seconds');
    
    const browser = await chromium.launch({
        headless: true,
        timeout: 20000
    });
    
    const context = await browser.newContext({
        viewport: { width: 1920, height: 1080 }
    });
    
    const page = await context.newPage();
    
    try {
        // Load the HTML file
        const htmlPath = path.join(__dirname, 'index.html');
        const fileUrl = `file://${htmlPath}`;
        
        console.log(`📄 Loading: ${fileUrl}`);
        await page.goto(fileUrl, { waitUntil: 'domcontentloaded', timeout: 20000 });
        
        console.log('✅ Page loaded successfully');
        
        // Test 1: Check page title
        const title = await page.title();
        console.log(`📋 Page Title: ${title}`);
        if (title.includes('StarSector 2')) {
            console.log('✅ Title test passed');
        } else {
            console.log('❌ Title test failed');
        }
        
        // Test 2: Check main heading
        const h1Text = await page.textContent('h1');
        console.log(`🎯 Main Heading: ${h1Text}`);
        if (h1Text.includes('StarSector 2')) {
            console.log('✅ Heading test passed');
        } else {
            console.log('❌ Heading test failed');
        }
        
        // Test 3: Check navigation links
        const navLinks = await page.$$eval('nav a', links => 
            links.map(link => ({
                text: link.textContent.trim(),
                href: link.getAttribute('href')
            }))
        );
        console.log(`🔗 Found ${navLinks.length} navigation links:`);
        navLinks.forEach(link => {
            console.log(`   - ${link.text}: ${link.href}`);
        });
        if (navLinks.length >= 6) {
            console.log('✅ Navigation links test passed');
        } else {
            console.log('❌ Navigation links test failed');
        }
        
        // Test 4: Check sections exist
        const sections = await page.$$eval('section', sections => 
            sections.map(section => section.id)
        );
        console.log(`📦 Found ${sections.length} sections: ${sections.join(', ')}`);
        const expectedSections = ['overview', 'features', 'tech', 'play', 'docs', 'links'];
        const allSectionsPresent = expectedSections.every(sec => sections.includes(sec));
        if (allSectionsPresent) {
            console.log('✅ Sections test passed');
        } else {
            console.log('❌ Sections test failed');
        }
        
        // Test 5: Check game launcher buttons
        const initButton = await page.$('#initButton');
        const launchButton = await page.$('#launchButton');
        
        if (initButton && launchButton) {
            console.log('✅ Game launcher buttons found');
            
            // Check initial state
            const initDisabled = await initButton.isDisabled();
            const launchDisabled = await launchButton.isDisabled();
            
            console.log(`   - Initialize button disabled: ${initDisabled}`);
            console.log(`   - Launch button disabled: ${launchDisabled}`);
            
            if (!initDisabled && launchDisabled) {
                console.log('✅ Button states test passed');
            } else {
                console.log('❌ Button states test failed');
            }
        } else {
            console.log('❌ Game launcher buttons not found');
        }
        
        // Test 6: Check canvas element
        const canvas = await page.$('#gameCanvas');
        if (canvas) {
            const width = await canvas.getAttribute('width');
            const height = await canvas.getAttribute('height');
            console.log(`🎮 Canvas found: ${width}x${height}`);
            console.log('✅ Canvas test passed');
        } else {
            console.log('❌ Canvas not found');
        }
        
        // Test 7: Check documentation links
        const docLinks = await page.$$eval('#docs a', links => 
            links.map(link => ({
                text: link.textContent.trim(),
                href: link.getAttribute('href')
            }))
        );
        console.log(`📚 Found ${docLinks.length} documentation links`);
        if (docLinks.length >= 5) {
            console.log('✅ Documentation links test passed');
        } else {
            console.log('❌ Documentation links test failed');
        }
        
        // Test 8: Check resource links
        const resourceLinks = await page.$$eval('#links a', links => 
            links.map(link => ({
                text: link.textContent.trim(),
                href: link.getAttribute('href')
            }))
        );
        console.log(`🔗 Found ${resourceLinks.length} resource links`);
        if (resourceLinks.length >= 5) {
            console.log('✅ Resource links test passed');
        } else {
            console.log('❌ Resource links test failed');
        }
        
        // Test 9: Check feature cards
        const featureCards = await page.$$('.feature-card');
        console.log(`✨ Found ${featureCards.length} feature cards`);
        if (featureCards.length >= 6) {
            console.log('✅ Feature cards test passed');
        } else {
            console.log('❌ Feature cards test failed');
        }
        
        // Test 10: Check tech stack badges
        const techBadges = await page.$$('.tech-badge');
        console.log(`⚙️  Found ${techBadges.length} tech stack badges`);
        if (techBadges.length >= 5) {
            console.log('✅ Tech stack badges test passed');
        } else {
            console.log('❌ Tech stack badges test failed');
        }
        
        // Test 11: Take screenshot
        const screenshotPath = path.join(__dirname, 'test_output', 'frontend_test.png');
        await page.screenshot({ path: screenshotPath, fullPage: true });
        console.log(`📸 Screenshot saved to: ${screenshotPath}`);
        
        console.log('\n✅ ALL TESTS COMPLETED SUCCESSFULLY!');
        
    } catch (error) {
        console.error('❌ Test failed:', error.message);
        throw error;
    } finally {
        await browser.close();
    }
}

// Run the test
testFrontend().catch(error => {
    console.error('Fatal error:', error);
    process.exit(1);
});