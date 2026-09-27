'use strict';

// Explicit, bounded CI experiment. Both profiles can run without a physical GPU;
// driver mode selects SwiftShader for Chrome's GL driver rather than only WebGL.
// Unknown values fail closed and there is no silent fallback after a failed test.
function browserRendererProfile(value = process.env.STARSECTOR_BROWSER_RENDERER || 'default') {
  if (value === 'default') return { name: value, args: ['--enable-unsafe-swiftshader'] };
  if (value === 'swiftshader-driver') return { name: value,
    args: ['--enable-unsafe-swiftshader', '--enable-gpu', '--use-gl=angle', '--use-angle=swiftshader'] };
  throw new Error('Unsupported STARSECTOR_BROWSER_RENDERER: '+value);
}

async function readBrowserGpuIdentity(browser, profile) {
  const session = await browser.newBrowserCDPSession();
  try {
    const { gpu } = await session.send('SystemInfo.getInfo');
    const fields = ['vendorId','deviceId','vendorString','deviceString','driverVendor','driverVersion'];
    return { requested: profile.name, args: profile.args, browserVersion: browser.version(),
      featureStatus: gpu.featureStatus,
      devices: gpu.devices.map(device=>Object.fromEntries(fields.filter(k=>device[k]!==undefined).map(k=>[k,device[k]]))),
      gl: Object.fromEntries(['glVendor','glRenderer','glVersion','displayType'].filter(k=>gpu.auxAttributes?.[k]!==undefined).map(k=>[k,gpu.auxAttributes[k]])) };
  } finally { await session.detach(); }
}
module.exports = { browserRendererProfile, readBrowserGpuIdentity };
