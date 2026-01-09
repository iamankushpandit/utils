<template>
  <div class="agent-page">
    <h1>Utility Agent</h1>
    <p class="intro">
      This AI-powered assistant can help you find data, compare regions, and explain utility metrics.
    </p>

    <div class="agent-container">
      <UtilAgentPanel />
    </div>
  </div>
</template>

<script>
import UtilAgentPanel from '../components/UtilAgentPanel.vue'

export default {
  name: 'AgentPage',
  components: {
    UtilAgentPanel
  }
}
</script>

<style scoped>
.agent-page {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

h1 {
  color: var(--ink);
  font-family: 'Sora', sans-serif;
  font-size: 2.5rem;
  margin-bottom: 1rem;
}

.intro {
  color: var(--ink-muted);
  font-size: 1.1rem;
  margin-bottom: 2rem;
  max-width: 800px;
}

.agent-container {
  background: white;
  border-radius: 12px;
  box-shadow: var(--shadow);
  padding: 2rem;
}
</style>
