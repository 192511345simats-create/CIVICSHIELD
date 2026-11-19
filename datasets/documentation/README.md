# CIVICSHIELD — Dataset Folder Guide

This folder stores infrastructure data used for research, testing, and AI model training.
It is completely separate from citizen complaints submitted through the application.

---

## Folder Structure

```
datasets/
├── raw/              # Original unprocessed data (do not modify)
├── processed/        # Cleaned and labelled data ready for use
├── import/           # Files staged for import into the application database
└── documentation/    # This README and data dictionaries
```

---

## Data Types

| Field (DataType) | Meaning |
|-----------------|---------|
| `CITIZEN_REPORT` | Real complaint submitted by a registered citizen |
| `SAMPLE_DATA` | Synthetic data for testing or demonstration |
| `HISTORICAL_DATASET` | Historical records imported from external sources |

The `DataType` field on every Complaint record ensures that
real citizen reports are never mixed with imported or sample data
in the application logic or analytics.

---

## Suggested Data Sources

| Source | Type | URL |
|--------|------|-----|
| data.gov.in | Government datasets | https://data.gov.in |
| Smart City Mission portals | City infrastructure data | https://smartcities.gov.in |
| Kaggle — Road Damage Dataset | Labelled road images | https://www.kaggle.com/datasets |
| OpenStreetMap | Map and location data | https://www.openstreetmap.org |
| NPTEL / IIT datasets | Infrastructure survey data | https://nptel.ac.in |

---

## Import Notes

- Do not place files directly in `raw/` that are still being processed.
- Use `processed/` only for data that has been cleaned, validated, and de-duplicated.
- Use `import/` for CSV or SQL files ready to be loaded into the `civicshield_db` database.
- All imported records must set `data_type = 'HISTORICAL_DATASET'` or `'SAMPLE_DATA'`.
- Never set `data_type = 'CITIZEN_REPORT'` on imported records.

---

*CIVICSHIELD Dataset Documentation — maintained by the CIVICSHIELD admin team.*
