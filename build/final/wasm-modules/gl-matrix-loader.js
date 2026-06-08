if (!globalThis.glMatrix) {
    await new Promise((resolve, reject) => {
        const existing = document.querySelector('script[data-starsector-gl-matrix="1"]');
        if (existing) {
            existing.addEventListener('load', () => resolve(), { once: true });
            existing.addEventListener('error', event => reject(event), { once: true });
            return;
        }

        const script = document.createElement("script");
        script.src = new URL("./gl-matrix-umd.js", import.meta.url).href;
        script.async = true;
        script.dataset.starsectorGlMatrix = "1";
        script.addEventListener('load', () => resolve(), { once: true });
        script.addEventListener('error', event => reject(event), { once: true });
        document.head.appendChild(script);
    });
}

export default globalThis.glMatrix;
