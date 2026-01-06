# Utility Explorer UI

Vue.js frontend for the Utility Explorer dashboard.

## Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Copy environment file:
   ```bash
   cp .env.template .env
   ```

3. Start development server:
   ```bash
   npm run dev
   ```

4. Open http://localhost:5173

## Build for Production

```bash
npm run build
```

## Project Structure

- `src/views/` - Page components (MapExplorer, Transparency)
- `src/components/` - Reusable components
- `src/services/` - API communication
- `public/` - Static assets