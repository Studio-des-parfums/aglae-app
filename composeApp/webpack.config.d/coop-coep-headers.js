// Skiko a besoin de SharedArrayBuffer pour initialiser le canvas wasmJs, qui n'est exposé par le
// navigateur que sur une page cross-origin isolée (voir commit "Add COOP/COEP headers for wasmJs
// cross-origin isolation" pour Vercel) : sans ces headers aussi en dev, le module wasm échoue
// silencieusement et le canvas reste vide.
config.devServer = config.devServer || {};
config.devServer.headers = {
    "Cross-Origin-Opener-Policy": "same-origin",
    "Cross-Origin-Embedder-Policy": "require-corp"
};
