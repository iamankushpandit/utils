# SOURCES.md — Utility Explorer

This project uses **free public sources** only. We do not invent or forecast data.
If a data point is missing, we display "No data available."

Each source entry must include:
- Official documentation URL
- Terms/license URL (or explicit statement that it's not available)
- Update cadence (how often it changes, and how often we check)
- Supported geography levels (STATE/COUNTY/PLACE)
- Supported time granularity (MONTH/YEAR/etc.)
- Units
- Change detection method (etag/last-modified/release id/latest period)
- Notes on known limitations

---

## Source: U.S. Energy Information Administration (EIA) - Electricity Pricing
- Source ID: EIA
- Official docs: https://www.eia.gov/electricity/data/state/
- Terms/license: https://www.eia.gov/about/copyrights_reuse.php (Public Domain)
- Update cadence: Monthly data, updated ~45 days after month end, check weekly
- Geo levels: STATE (2-digit FIPS)
- Granularity: MONTH
- Units: cents/kWh (average retail price)
- Change detection: Latest available period vs stored data
- Notes: State-level averages only, no county/city breakdown available

---

## Source: (placeholder) FCC Broadband Availability
- Source ID: fcc_broadband (placeholder)
- Official docs: https://www.fcc.gov/general/broadband-deployment-data-fcc-form-477
- Terms/license: Public Domain
- Update cadence: Biannual (June/December), check monthly
- Geo levels: STATE, COUNTY, PLACE (Census blocks aggregated)
- Granularity: BIANNUAL
- Units: Percentage coverage at 25/3 Mbps
- Change detection: Form 477 filing period vs stored data
- Notes: Coverage percentages, not actual speeds

---

## Source: (placeholder) EPA Water Quality
- Source ID: epa_water (placeholder)
- Official docs: https://www.epa.gov/ground-water-and-drinking-water/safe-drinking-water-information-system-sdwis-federal-reporting
- Terms/license: Public Domain
- Update cadence: Quarterly compliance reports, check monthly
- Geo levels: Varies by system (aggregated to COUNTY/STATE)
- Granularity: QUARTER
- Units: Violation counts, compliance percentages
- Change detection: Latest reporting period vs stored data
- Notes: Public water systems only, private wells not included