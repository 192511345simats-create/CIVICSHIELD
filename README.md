# CIVICSHIELD — Setup & Run Guide

**Intelligent Infrastructure Failure and Public Safety Reporting System**

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 17 | Eclipse Adoptium Temurin recommended |
| Maven | 3.9.x | At `C:\Users\jenef\apache-maven-3.9.6` |
| MySQL | 8.0 | Server must be running before launch |
| Browser | Any modern | Chrome, Firefox, Edge |

---

## 1. Configure the Database

Open `src/main/resources/application.properties` and set your MySQL password:

```properties
spring.datasource.password=YOUR_ACTUAL_MYSQL_PASSWORD
```

> The database `civicshield_db` is created automatically on first run  
> (via `createDatabaseIfNotExist=true` in the JDBC URL).  
> All tables are created automatically by Hibernate (`ddl-auto=update`).

---

## 2. Run the Application

From the project directory:

```
C:\Users\jenef\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run -f "C:\Users\jenef\.gemini\antigravity\scratch\civicshield\pom.xml"
```

Or if you're already inside the project directory:

```
C:\Users\jenef\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
```

The application starts at: **http://localhost:8080**

---

## 3. Default Admin Account

An administrator account is created automatically on first startup.

| Field | Value |
|-------|-------|
| Email | `admin@civicshield.gov` |
| Password | `Admin@123` |
| URL | `http://localhost:8080/login` |

> **Important:** Change the admin password immediately after first login.  
> Admin credentials are logged to the console on startup — check the logs.

---

## 4. Application Pages

| URL | Purpose |
|-----|---------|
| `http://localhost:8080` | Public landing page |
| `http://localhost:8080/login` | Login (citizen & admin) |
| `http://localhost:8080/register` | Citizen registration |
| `http://localhost:8080/citizen/home` | Citizen dashboard |
| `http://localhost:8080/admin/overview` | Admin dashboard |

---

## 5. File Uploads

Uploaded photos are stored in the `uploads/` directory inside the project root.

To change the upload location, update `application.properties`:

```properties
app.upload.dir=uploads
```

Uploaded images are served at: `http://localhost:8080/uploads/{path}`

---

## 6. Google OAuth 2.0 (Optional)

To enable **Continue with Google** login:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project → Enable the **Google+ API** (or **Google Identity**)
3. Create OAuth 2.0 credentials (Web Application type)
4. Set the redirect URI to: `http://localhost:8080/login/oauth2/code/google`
5. Copy your Client ID and Client Secret
6. In `application.properties`, uncomment and fill:

```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
spring.security.oauth2.client.registration.google.redirect-uri=http://localhost:8080/login/oauth2/code/google
spring.security.oauth2.client.registration.google.scope=email,profile
```

> **Never commit** your Client ID or Client Secret to version control.

---

## 7. Intelligent Features

CIVICSHIELD includes the following automatic features:

| Feature | How it works |
|---------|-------------|
| **Image Duplicate Detection** | SHA-256 exact match + perceptual hashing (pHash DCT) |
| **EXIF Extraction** | GPS coordinates, timestamp, camera info via metadata-extractor |
| **Duplicate Complaint Detection** | Jaccard similarity on description + Haversine distance |
| **Risk Scoring** | Weighted score (Severity 35% + Exposure 25% + Duration 20% + Confirmations 10% + Location 10%) |
| **Priority Assignment** | Automatic LOW / MEDIUM / HIGH / CRITICAL based on risk score |
| **Escalation Scheduler** | Hourly check for overdue assignments — auto-escalates |
| **Hotspot Detection** | Grid-based clustering (≈100m cells, threshold = 3 complaints) |

---

## 8. Seeded Departments

6 departments are created automatically on first startup:

1. Roads and Infrastructure
2. Streetlights and Electrical
3. Water and Drainage
4. Parks and Public Spaces
5. Public Safety and Emergency
6. Waste Management

---

## 9. Project Structure

```
civicshield/
├── src/main/java/com/civicshield/
│   ├── algorithm/          # Risk scoring, duplicate detection, hotspot detection
│   ├── config/             # Security config, data initializer
│   ├── controller/         # Web controllers (Auth, Citizen, Admin, Upload)
│   ├── dto/                # Data Transfer Objects
│   ├── entity/             # JPA entities (User, Complaint, Assignment, ...)
│   ├── exception/          # Global exception handler
│   ├── imageverification/  # Image processing service (SHA-256, pHash, EXIF)
│   ├── notification/       # Notification service
│   ├── repository/         # Spring Data JPA repositories
│   ├── scheduler/          # Escalation scheduler
│   ├── security/           # Spring Security + OAuth2 configuration
│   ├── service/            # Business logic services
│   └── util/               # File storage, complaint ID generator
├── src/main/resources/
│   ├── static/
│   │   ├── css/            # civicshield.css — all styles
│   │   └── images/         # civicshield-logo.png
│   ├── templates/
│   │   ├── admin/          # 11 admin dashboard pages
│   │   ├── auth/           # Login, register, forgot password
│   │   ├── citizen/        # 7 citizen dashboard pages
│   │   ├── error/          # 403, 404, 500 error pages
│   │   └── landing/        # Public homepage
│   └── application.properties
├── datasets/               # Folder structure for training/historical data
├── uploads/                # Uploaded complaint photos (created at runtime)
└── pom.xml
```

---

## 10. Troubleshooting

| Problem | Solution |
|---------|---------|
| `Access denied for user 'root'@'localhost'` | Set correct MySQL password in `application.properties` |
| Port 8080 already in use | Change `server.port=8081` in `application.properties` |
| `No suitable driver found` | Ensure MySQL 8.0 Connector/J is in `pom.xml` (it is by default) |
| Images not loading | Check that `uploads/` directory exists and is writable |
| Google login fails | Verify OAuth credentials and redirect URI in `application.properties` |
| Tables not created | Confirm `spring.jpa.hibernate.ddl-auto=update` and that the DB exists |

---

*CIVICSHIELD — Report • Track • Build a Safer City*
