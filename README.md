# Starsector WebVM & Native Oracle VM

A high-performance implementation of Starsector running in the browser via WebVM (CheerpJ) and natively on Oracle Cloud VM.

## 🚀 Live Demo (GitHub Pages)
[Starsector WebVM](https://adybag14-cyber.github.io/starsectorquick/)

## 🏗️ Native Oracle VM Verification
The game is verified to run natively on Oracle Cloud Infrastructure (Ubuntu 20.04).

### Key Features
- **Native LWJGL 2 Execution**: Optimized for Oracle VM's virtual framebuffer (Xvfb).
- **NoVNC Streaming**: Real-time stream accessible via Port 8000.
- **Hardware Acceleration**: Mesa software rendering (Mesa 21.2+) with GL 3.0 override.
- **Resolved Dependencies**: Custom linkage for AWT, OpenAL, and LWJGL natives.

### Debugging & Evidence
- **Launcher Stability**: Verified textures and font loading in `starsector.log`.
- **Process Monitoring**: Native Java process maintains stable 50-90% CPU during execution.
- **Visual Proof**: Screenshots and videos captured directly from the VM display.

## 🛠️ Deployment Instructions
1.  **Clone the Repo**: `git clone https://github.com/adybag14-cyber/starsectorquick.git`
2.  **Native Launch**: Use `bash mega_relaunch.sh` on the VM to start the Xvfb/noVNC stack and game.
3.  **WebVM Build**: Run `node server.js` to serve the CheerpJ version.

## 📝 Documentation
- [WASM Modules](build/final/wasm-modules/): Optimized binaries for WebVM.
- [Jars](jars/): Complete classpath for native and web execution.
- [Natives](native/linux/): Sideloaded X11 and OpenAL libraries.

---
*Verified on Sunday, 25 January 2026.*
