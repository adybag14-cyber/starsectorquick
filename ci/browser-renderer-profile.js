'use strict';

// Explicit, bounded CI profiles. The existing default and SwiftShader driver
// profiles remain CPU-capable. D3D11 hardware is an opt-in Windows profile;
// its observed renderer identity must confirm hardware instead of a fallback.
// Unknown values fail closed and there is no silent fallback after a failed test.
function browserRendererProfile(value = process.env.STARSECTOR_BROWSER_RENDERER || 'default') {
  if (value === 'default') return { name: value, args: ['--enable-unsafe-swiftshader'] };
  if (value === 'swiftshader-driver') return { name: value,
    args: ['--enable-unsafe-swiftshader', '--enable-gpu', '--use-gl=angle', '--use-angle=swiftshader'] };
  if (value === 'mesa-llvmpipe') return { name: value,
    args: ['--enable-gpu', '--use-gl=angle', '--use-angle=gl', '--ozone-platform=x11', '--ignore-gpu-blocklist'] };
  if (value === 'd3d11-hardware') return { name: value,
    args: ['--enable-gpu', '--use-gl=angle', '--use-angle=d3d11'] };
  throw new Error('Unsupported STARSECTOR_BROWSER_RENDERER: '+value);
}

async function readBrowserGpuIdentity(browser, profile) {
  const session = await browser.newBrowserCDPSession();
  try {
    const { gpu } = await session.send('SystemInfo.getInfo');
    const fields = ['vendorId','deviceId','vendorString','deviceString','driverVendor','driverVersion'];
    const identity = { requested: profile.name, args: profile.args, browserVersion: browser.version(),
      featureStatus: gpu.featureStatus,
      devices: gpu.devices.map(device=>Object.fromEntries(fields.filter(k=>device[k]!==undefined).map(k=>[k,device[k]]))),
      gl: Object.fromEntries(['glVendor','glRenderer','glVersion','displayType'].filter(k=>gpu.auxAttributes?.[k]!==undefined).map(k=>[k,gpu.auxAttributes[k]])) };
    for (const [name, expected] of [['swiftshader-driver', /SwiftShader/i], ['mesa-llvmpipe', /llvmpipe/i]]) {
      if (profile.name === name && !expected.test(identity.gl.glRenderer || '')) {
        throw new Error('Requested '+name+' renderer is not active: '+JSON.stringify(identity));
      }
    }
    if (profile.name === 'd3d11-hardware') {
      // Adapter enumeration may include an unused Microsoft software adapter.
      // Validate the active GL renderer and WebGL status, not every device.
      const renderer = identity.gl.glRenderer || '';
      const software = /SwiftShader|llvmpipe|softpipe|software|Microsoft Basic Render|\bWARP\b/i.test(renderer);
      if (!/Direct3D11|D3D11/i.test(renderer) || software
          || /software/i.test(identity.featureStatus?.webgl || '')) {
        throw new Error('Requested Direct3D11 hardware renderer is not active: '+JSON.stringify(identity));
      }
    }
    return identity;
  } finally { await session.detach(); }
}
module.exports = { browserRendererProfile, readBrowserGpuIdentity };
