<template>
  <div id="app">
    <nav class="navbar">
      <div class="nav-brand">
        <router-link to="/">Utility Explorer</router-link>
      </div>
      <div class="nav-links">
        <router-link to="/">Map</router-link>
        <router-link to="/transparency">Transparency</router-link>
      </div>
    </nav>
    
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Sora:wght@400;500;600;700&display=swap');

:root {
  --ink: #0b1f1a;
  --ink-muted: #36514f;
  --accent: #1a8a7a;
  --accent-strong: #0f6d61;
  --accent-warm: #f0b44f;
  --surface: #f9f6f1;
  --surface-2: #ffffff;
  --surface-3: #f1ece5;
  --border: #e2dad0;
  --shadow: 0 10px 30px rgba(10, 25, 22, 0.08);
  --success: #2f8f68;
  --warning: #c8892a;
  --danger: #b14c4c;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Sora', sans-serif;
  color: var(--ink);
  background:
    radial-gradient(900px 420px at 15% -10%, rgba(31, 138, 122, 0.18), transparent 60%),
    radial-gradient(700px 360px at 85% 0%, rgba(240, 180, 79, 0.18), transparent 55%),
    linear-gradient(180deg, #f7f2ea 0%, #efe7dc 100%);
}

#app {
  min-height: 100vh;
}

.navbar {
  background: linear-gradient(90deg, #0f2d29 0%, #123a35 45%, #194840 100%);
  border-bottom: 1px solid rgba(15, 45, 41, 0.4);
  padding: 1.25rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--shadow);
}

.nav-brand a {
  font-size: 1.5rem;
  font-weight: bold;
  color: #f7f2ea;
  text-decoration: none;
  letter-spacing: 0.4px;
}

.nav-links {
  display: flex;
  gap: 2rem;
}

.nav-links a {
  color: rgba(247, 242, 234, 0.75);
  text-decoration: none;
  padding: 0.5rem 0.75rem;
  border-radius: 999px;
  transition: background-color 0.2s, color 0.2s;
}

.nav-links a:hover,
.nav-links a.router-link-active {
  background-color: rgba(255, 255, 255, 0.12);
  color: #ffffff;
}

.main-content {
  padding: 2.5rem 2rem 4rem;
  max-width: 1200px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 24px;
  box-shadow: var(--shadow);
  margin-top: 2rem;
  backdrop-filter: blur(8px);
}
</style>
