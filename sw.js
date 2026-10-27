/* Suivi ACM : fonctionnement hors ligne */
const VERSION = "suivi-acm-v2.1.0";
const FONTS = "carnet-acm-polices";
const CORE = ["./", "./index.html", "./manifest.webmanifest", "./icon-192.png", "./icon-512.png", "./icon-maskable-512.png", "./apple-touch-icon.png"];
const MINE = k => /^(suivi-acm|carnet-acm)/.test(k);
self.addEventListener("install", e => {
  e.waitUntil(caches.open(VERSION).then(c => c.addAll(CORE)).then(() => self.skipWaiting()));
});
self.addEventListener("activate", e => {
  // ne supprime que les anciens caches de cette application (pas ceux des autres applis du même site)
  e.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => MINE(k) && k !== VERSION && k !== FONTS).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener("fetch", e => {
  const req = e.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.origin === self.location.origin){
    if (!url.pathname.startsWith(new URL("./", self.location).pathname)) return;
    e.respondWith(caches.open(VERSION).then(async cache => {
      const hit = await cache.match(req, {ignoreSearch:true}) || (req.mode === "navigate" ? await cache.match("./") : null);
      const net = fetch(req).then(res => { if (res && res.ok) cache.put(req, res.clone()); return res; }).catch(() => hit);
      return hit || net;
    }));
  } else if (/fonts\.(googleapis|gstatic)\.com$/.test(url.hostname)){
    e.respondWith(caches.open(FONTS).then(async cache => {
      const hit = await cache.match(req); if (hit) return hit;
      try { const res = await fetch(req); cache.put(req, res.clone()); return res; } catch (err){ return Response.error(); }
    }));
  }
});
