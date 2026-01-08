# UX & UI Patterns Memory Bank

## Core User Personas
- **Decision Maker** (primary): Compares regions quickly, wants credible sourcing
- **Analyst/Researcher**: Wants exportable data with provenance
- **Developer** (secondary): Consumes API, validates sources, builds integrations

## Map-First UI Principles
- **Choropleth coloring**: Only where chosen source supports that geography
- **Drilldown**: US → State → County → City (Census Place) for boundaries
- **No data invention**: Show "No data available" vs empty/estimated values
- **Hover tooltips**: Region name + value + unit + period + retrieved timestamp

## Key UI Components
- **Map Explorer**: Primary interface with layer/source/geo/period selectors
- **Region Details Drawer**: Current value + provenance + time series chart + CSV export
- **Transparency Page**: Ideology + methodology + live data status + source links
- **Util Agent Panel**: Read-only query interface with examples + citations

## Visual Design Rules
- **Color scale**: Green (low) → Red (high) for choropleth
- **Legend requirements**: Min/max + unit + period + retrieved_at timestamp
- **Provenance display**: Source name + attribution + terms link visible everywhere
- **No data states**: Keep boundaries visible, disable fill, show clear messaging

## Interaction Patterns
- **Drilldown behavior**: If no data at deeper level, show boundaries but no values
- **Source comparison**: Show multiple sources separately, never blend silently
- **Export functionality**: CSV with full provenance fields included
- **Util Agent safety**: Show "Insufficient data" vs partial/estimated results

## Transparency Requirements
- **Data Status Widget**: Live source status (last success, next run, errors)
- **Methodology Page**: Clear explanation of what we do/don't do
- **Source Attribution**: Links to original terms + attribution text
- **Coverage Limitations**: Honest about geographic/temporal gaps

## Responsive Design
- **Mobile-first**: Map + controls work on mobile devices
- **Progressive enhancement**: Core functionality without JavaScript
- **Accessibility**: WCAG compliance for government/institutional users
