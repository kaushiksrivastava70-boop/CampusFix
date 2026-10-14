# CampusFix — Campus Operations & Issue Management Platform

CampusFix helps students report real-world campus problems and helps campus staff triage, assign, track, and resolve them. This enhanced starter includes a React frontend, a Java 17 / Spring Boot REST API, and Tailwind CSS + Bootstrap styling.

## Features

- Operations dashboard with live ticket counts, resolution rate, and high-priority watch
- Issue creation with category, campus location, priority, and description
- Search by title, description, location, ticket ID, and assigned team
- Filter by status, priority, and category; sort by newest, oldest, recently updated, or priority
- Ticket details modal with timestamps and activity timeline
- Update status, priority, and assigned team
- Internal comments / notes on each ticket
- Delete ticket with confirmation
- Analytics for status and category breakdown
- CSV export of the currently filtered issue list
- Auto-refresh every 15 seconds and responsive mobile layout
- API health endpoint at `/api/health`

## Requirements

- JDK 17 or later
- Apache Maven 3.8+
- Node.js 18 or later
- Internet access on first run so Maven and npm can download dependencies

## 1. Start the Java backend

Open a terminal in this folder:

```powershell
cd backend
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

If PowerShell says `mvn` is not recognized, either add Maven's `bin` folder to your Windows `Path`, or run Maven directly. For example, if Maven is extracted under Downloads as in the setup guide:

```powershell
& "$HOME\Downloads\apache-maven-3.10.0-bin\apache-maven-3.10.0\bin\mvn.cmd" spring-boot:run
```

If Maven says `JAVA_HOME` is not defined correctly, set it in the current PowerShell session first:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Keep the backend terminal open. Example API endpoints:

- `GET http://localhost:8080/api/health`
- `GET http://localhost:8080/api/issues`
- `GET http://localhost:8080/api/stats`
- `POST http://localhost:8080/api/issues`
- `PATCH http://localhost:8080/api/issues/1001`
- `POST http://localhost:8080/api/issues/1001/comments`
- `DELETE http://localhost:8080/api/issues/1001`

## 2. Start the frontend

Open a second terminal at the project root:

```powershell
cd frontend
npm install
npm run dev
```

Open the local URL Vite prints, usually `http://localhost:5173`.

## Notes / limitations

- The backend currently uses in-memory demo data. New reports, comments, assignments, and status changes reset when the Java server restarts. Add a database such as MySQL or PostgreSQL for persistent data.
- The app currently has a demo admin workspace, not production authentication. Before public deployment, add student/staff login, role-based permissions, input validation, persistent storage, and secure file uploads if photos are added.
- Tailwind and Bootstrap are both included as requested; custom CSS provides the dashboard theme.
