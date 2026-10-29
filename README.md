# SlotWise — Room and Equipment Booking System

> **Academic B.Tech DevOps Case Study Project**  
> A full-stack campus facility and equipment reservation platform built with **Java 21**, **Spring Boot 3.3.2**, **Spring Security 6**, **Spring Data MongoDB**, **MongoDB 8.x**, **Thymeleaf**, and **Vanilla HTML5/CSS3/JavaScript**.

---

## 1. Project Overview

**SlotWise** is an academic web application designed for universities and colleges to streamline how students, faculty, and academic departments discover and book campus facilities (such as seminar rooms, conference halls, computer laboratories) and shared multimedia equipment (such as 4K laser projectors and portable sound systems).

The system replaces manual paper logs and error-prone verbal scheduling with automated, persistent, conflict-free booking logic backed by MySQL relational storage.

---

## 2. Problem Statement & Objectives

### The Problem
In academic institutions, shared rooms and high-demand equipment frequently suffer from:
- **Double-booking conflicts:** Two faculty members or student clubs reserving the same room at the same time.
- **Opacity:** Students cannot see whether a room or equipment is available before walking to the administrative department.
- **Lack of audit trail:** Inability to track who booked what resource, for what purpose, and when.
- **Unauthorized overrides:** Students canceling or tampering with other users' bookings.

### Objectives
1. **Automated Conflict Prevention:** Strictly reject any booking overlapping an existing confirmed reservation on the same resource and date at the backend database query level.
2. **Granular Role-Based Access Control (RBAC):** Provide distinct experiences and permissions for Students/Staff vs. Facility Administrators.
3. **Transparent Facility Exploration:** Offer real-time search, category filtering (Rooms vs. Equipment), and slot availability inspection before booking.
4. **Persistent Reliability:** Store records in MySQL with relational foreign keys, unique constraints, and indexes.
5. **Clean Architecture for DevOps:** Lay a solid, monolithic Spring Boot foundation suitable for subsequent containerization (Docker), CI/CD automation (Jenkins), and orchestration (Kubernetes).

---

## 3. Technology Stack

| Layer | Technologies Used |
|---|---|
| **Backend Framework** | Java 21, Spring Boot 3.3.2 |
| **Web MVC & Templates** | Spring MVC, Thymeleaf 3.1, Thymeleaf Spring Security 6 dialect |
| **Security & Auth** | Spring Security 6 (Session-based, BCrypt password hashing, RBAC) |
| **Persistence & Database** | Spring Data MongoDB, MongoDB 8.x |
| **Primary Database** | MongoDB (Runs on `mongodb://localhost:27017/slotwise_db`) |
| **Frontend UI** | Semantic HTML5, Custom Responsive CSS3 Design System, Vanilla JavaScript |
| **Build Tool** | Apache Maven 3.9+ |
| **Testing** | JUnit 5 (Jupiter), Mockito, Spring Security Test, Spring Boot Test (MockMvc) |

---

## 4. Application Architecture

SlotWise follows a clean, layered architectural design adhering to Separation of Concerns:

```
+-----------------------------------------------------------------------------------+
|                           Client Browser (HTML5 / CSS3 / JS)                     |
+-----------------------------------------------------------------------------------+
                                         │  HTTP / Form Submissions
                                         ▼
+-----------------------------------------------------------------------------------+
|                           Spring Security Filter Chain                            |
|     (BCrypt Authentication, Session Management, Role Authorization Filter)        |
+-----------------------------------------------------------------------------------+
                                         │
        ┌────────────────────────────────┼────────────────────────────────┐
        ▼                                ▼                                ▼
+--------------------+         +--------------------+         +--------------------+
|  AuthController    |         | ResourceController |         | BookingController  |
|  HomeController    |         | AdminResourceCtrl  |         | AdminBookingCtrl   |
| DashboardController|         |                    |         |                    |
+--------------------+         +--------------------+         +--------------------+
        │                                │                                │
        └────────────────────────────────┼────────────────────────────────┘
                                         ▼
+-----------------------------------------------------------------------------------+
|                                  Service Layer                                    |
|   (UserService, ResourceService, BookingService, CustomUserDetailsService)       |
|    - Validates inputs (future dates, end > start, active resource checks)         |
|    - Conflict Engine: start < req.end AND end > req.start                         |
|    - Transaction Management (@Transactional isolation levels)                     |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼
+-----------------------------------------------------------------------------------+
|                                Repository Layer                                   |
|      (UserRepository, ResourceRepository, BookingRepository - Spring Data JPA)    |
+-----------------------------------------------------------------------------------+
                                         │
                                         ▼
+-----------------------------------------------------------------------------------+
|                         Relational Database (MySQL 8.x)                           |
|       Tables: users, resources, bookings  | Composite indexes for slot queries    |
+-----------------------------------------------------------------------------------+
```

---

## 5. Project Folder Structure

```
slotwise/
├── database-setup.sql                       # Standalone MySQL schema initialization script
├── pom.xml                                  # Maven project dependencies and build configuration
├── README.md                                # Comprehensive project guide & evaluation checklist
└── src/
    ├── main/
    │   ├── java/com/slotwise/
    │   │   ├── SlotWiseApplication.java     # Main Spring Boot entry point
    │   │   ├── config/
    │   │   │   ├── CustomAuthenticationSuccessHandler.java # Directs Admin to /admin, User to /dashboard
    │   │   │   ├── DataInitializer.java     # Seeds admin user & 5 sample resources on fresh startup
    │   │   │   └── SecurityConfig.java      # URL security rules, BCrypt bean, form login & logout
    │   │   ├── controller/
    │   │   │   ├── AdminBookingController.java    # Admin reservation management & revocations
    │   │   │   ├── AdminDashboardController.java  # Admin metric cards & overview
    │   │   │   ├── AdminResourceController.java   # Resource creation, editing, active toggle
    │   │   │   ├── AuthController.java      # Registration and Login endpoints
    │   │   │   ├── BookingController.java   # Booking reservation, conflict errors, My Bookings
    │   │   │   ├── DashboardController.java # Student user dashboard
    │   │   │   ├── HomeController.java      # Public landing page routing
    │   │   │   └── ResourceController.java  # Catalog browsing & availability filtering
    │   │   ├── dto/
    │   │   │   ├── AvailabilityCheckDto.java# Date, start/end time, keyword, location filter
    │   │   │   ├── BookingRequestDto.java   # Form binding for new booking requests
    │   │   │   ├── DashboardStatsDto.java   # Aggregated metrics for administrative cards
    │   │   │   ├── ResourceDto.java         # DTO for adding & updating facilities
    │   │   │   └── UserRegistrationDto.java # Registration form DTO with validation
    │   │   ├── entity/
    │   │   │   ├── Booking.java             # JPA Entity mapping to `bookings` table
    │   │   │   ├── BookingStatus.java       # Enum: CONFIRMED, CANCELLED
    │   │   │   ├── Resource.java            # JPA Entity mapping to `resources` table
    │   │   │   ├── ResourceType.java        # Enum: ROOM, EQUIPMENT
    │   │   │   ├── Role.java                # Enum: ROLE_STUDENT, ROLE_ADMIN
    │   │   │   └── User.java                # JPA Entity mapping to `users` table
    │   │   ├── exception/
    │   │   │   ├── BookingConflictException.java # Thrown when requested slot overlaps
    │   │   │   ├── GlobalExceptionHandler.java   # Unified exception handling & error views
    │   │   │   ├── InvalidBookingException.java  # Thrown on invalid past dates or times
    │   │   │   ├── ResourceNotFoundException.java# 404 resource lookup failure
    │   │   │   └── UnauthorizedActionException.java # 403 when user cancels others' bookings
    │   │   ├── repository/
    │   │   │   ├── BookingRepository.java   # Custom conflict query & historical lookup queries
    │   │   │   ├── ResourceRepository.java  # Multi-parameter search & active filtering
    │   │   │   └── UserRepository.java      # Unique email lookup & existence check
    │   │   └── service/
    │   │       ├── BookingService.java      # Core conflict prevention algorithm & cancellation
    │   │       ├── CustomUserDetails.java   # Spring Security UserDetails adapter
    │   │       ├── CustomUserDetailsService.java # Database-backed authentication loader
    │   │       ├── ResourceService.java     # Catalog search, availability, CRUD logic
    │   │       └── UserService.java         # Registration, BCrypt hashing, admin bootstrap
    │   └── resources/
    │       ├── application.properties       # Default profile (MySQL configuration)
    │       ├── application-local.properties # Standalone local evaluation profile
    │       ├── static/
    │       │   ├── css/style.css            # Custom responsive modern CSS design system
    │       │   └── js/main.js               # Vanilla JS date constraints & confirmation modals
    │       └── templates/
    │           ├── index.html               # Public welcome landing page
    │           ├── dashboard.html           # Student user dashboard
    │           ├── admin/
    │           │   ├── bookings.html        # Admin all-bookings management table
    │           │   ├── dashboard.html       # Admin control center & live statistics
    │           │   ├── resource-form.html   # Add/Edit resource form
    │           │   └── resources.html       # Admin resource catalog & active toggle
    │           ├── auth/
    │           │   ├── login.html           # Login card with test credentials guide
    │           │   └── register.html        # Registration form with readable field errors
    │           ├── bookings/
    │           │   ├── form.html            # Reservation booking form with conflict warning
    │           │   └── my-bookings.html     # User booking history & cancellation actions
    │           ├── error/
    │           │   └── error.html           # Friendly, informative error layout
    │           ├── fragments/
    │           │   └── layout.html          # Shared navbar, header, footer, user pill
    │           └── resources/
    │               ├── detail.html          # Facility details & daily schedule inspector
    │               └── list.html            # Resource listing with live availability filter
    └── test/
        ├── java/com/slotwise/
        │   ├── config/
        │   │   └── SecurityAccessIntegrationTest.java # Unauth & RBAC access barrier tests
        │   ├── controller/
        │   │   └── WebFlowIntegrationTest.java        # Full MVC & Thymeleaf end-to-end tests
        │   └── service/
        │       ├── BookingServiceTest.java  # Conflict checking, adjacent slots, cancellations
        │       ├── ResourceServiceTest.java # Resource CRUD & active toggle tests
        │       └── UserServiceTest.java     # User registration & password hashing tests
        └── resources/
            └── application.properties       # In-memory H2 configuration for automated tests
```

---

## 6. Core Features Implemented

### A. Authentication & Role-Based Access Control
- Secure user registration with full name, email, Student/Faculty role selection, password, and password confirmation check.
- Duplicate email prevention enforced at both business logic level and database unique constraint.
- Password hashing with **BCrypt** (cost factor 10) — passwords are never stored in plaintext.
- Session-based authentication with role redirection (Admins are directed to `/admin/dashboard`; Students and Faculty to `/dashboard`).
- Protected administrative URLs (`/admin/**`) — ordinary users attempting to access administrative endpoints receive a **403 Forbidden** status code.
- A light/dark theme toggle follows the system preference by default and remembers the user's selection across pages.

### B. Dynamic Resource Catalog & Inspection
- Database-backed resources categorized into **Room / Facility** and **Equipment / Device**.
- Detailed attributes: Name, Type, Description, Location, Capacity (optional for rooms), and Active status.
- Real-time search by keyword/name, category filter, and campus building/location filter.
- **Daily Schedule Inspector:** Users can select any target date to view already-confirmed bookings on a specific resource before submitting a request.

### C. Booking System & Conflict Engine
- Required parameters: Resource ID, Date, Start Time, End Time, and Purpose.
- **Rigorous Business Validations:**
  1. No blank required fields.
  2. Booking date and start time must be in the future.
  3. End time must be strictly after start time.
  4. Selected resource must be active.
- **Backend Conflict Check Algorithm:**
  For the same resource and date, an overlap occurs if and only if:
  $$\text{existing.startTime} < \text{requested.endTime} \quad \text{AND} \quad \text{existing.endTime} > \text{requested.startTime}$$
  - **Adjacent time slots are strictly allowed:** e.g., a booking from `10:00 - 11:00` followed immediately by `11:00 - 12:00` does not conflict.
  - **Simultaneous bookings for different resources are allowed:** Booking Room A from `10:00 - 11:00` does not block Projector 1 from `10:00 - 11:00`.
  - **Cancelled bookings release the slot immediately:** Queries check `status = CONFIRMED`, meaning cancelled reservations do not block subsequent bookings.

### D. My Bookings & Self-Service Cancellation
- Students can view their complete booking history with status badges (`Confirmed` in green, `Cancelled` in muted gray).
- Eligible bookings can be cancelled with a confirmation prompt (`confirm-cancel`).
- **Security Guardrail:** Ordinary users cannot cancel another user's reservation (`UnauthorizedActionException`).

### E. Administrator Control Center
- Live aggregate metric cards:
  - Active Campus Resources
  - Total Bookings Recorded
  - Upcoming Reservations
  - Cancelled Bookings
  - Total Registered Users
- **Resource Management:** Add new facilities, update specifications, and toggle active/inactive availability.
- **Booking Oversight:** View all reservations across campus with filtering by status and administrative override cancellation.

### F. Automated Sample Data Seeding
- On initial startup, if the database has no resources, `DataInitializer` automatically inserts 5 realistic campus facilities:
  1. `Seminar Room A` (Room, Capacity: 80, Building 1)
  2. `Conference Room` (Room, Capacity: 20, Administration Wing)
  3. `Computer Lab 1` (Room, Capacity: 40, IT Block)
  4. `Projector 1` (Equipment, AV Media Center)
  5. `Portable Speaker 1` (Equipment, Student Activities Office)
- Checks table row count first to guarantee sample data is never duplicated on application restart.

---

## 7. Prerequisites

1. **Java Development Kit (JDK):** Version 21 (or 17+)
   - Verify with: `java -version`
2. **Apache Maven:** Version 3.8+ (or use the included Maven wrapper / installed Maven)
   - Verify with: `mvn -version`
3. **MongoDB Community Server:** Version 6.0+ / 8.x
   - Running on `localhost:27017` (default port).

---

## 8. Database Setup Guide (MongoDB)

### Automatic Database Creation
MongoDB automatically creates databases and collections upon first write. No manual table or schema creation is required!

### Connection Configuration
The application connects automatically to your local MongoDB server:
`mongodb://localhost:27017/slotwise_db`

You can customize this if needed via the `MONGO_URI` environment variable or in `src/main/resources/application.properties`:
```properties
spring.data.mongodb.uri=${MONGO_URI:mongodb://localhost:27017/slotwise_db}
```

### Initial Administrator Account
On first launch, SlotWise automatically creates the default administrator account:
- **Email:** `admin@slotwise.edu`
- **Password:** `Admin@1234`
- **Demo Student:** `student@slotwise.edu` / `Student@1234`

---

## 9. How to Build & Run the Application

### 1. Build and Package:
```powershell
Set-Location "C:\Users\satya\.gemini\antigravity\scratch\slotwise"
mvn clean package -DskipTests
```

### 2. Run the Application:
Ensure MongoDB is running locally on port 27017, then execute:
```powershell
java -jar target/slotwise-1.0.0.jar
```
Or start directly using Maven:
```powershell
mvn spring-boot:run
```

### 3. Open in Browser:
Visit: **[http://localhost:8080](http://localhost:8080)** (or your configured port).

---

## 10. Default Evaluation Accounts

| Role | Email Address | Password | Permissions |
|---|---|---|---|
| **Administrator** | `admin@slotwise.edu` | `Admin@1234` | Full access: Admin dashboard, resource management, booking oversight |
| **Demo Student** | `student@slotwise.edu` | `Student@1234` | Student access: Browse resources, check availability, book slots, view & cancel own bookings |

*New students and faculty can register using the **Register** button and select their role.*

---

## 11. Automated Testing Suite

SlotWise includes a comprehensive test suite of **34 automated tests** covering all academic requirements. Tests use a dedicated test database profile to isolate test data.

### Running Tests:
```powershell
mvn test
```

### Test Coverage Summary:
- **`BookingServiceTest` (10 tests):**
  - `testSuccessfulBooking()`: Valid reservation creation when slot is free.
  - `testRejectionOfOverlappingBookingsSameResource()`: Overlapping slot rejection.
  - `testAllowAdjacentTimeSlots()`: Allows consecutive back-to-back bookings (`10-11` and `11-12`).
  - `testAllowSimultaneousTimeSlotsDifferentResources()`: Allows same slot for different facilities.
  - `testCancelledBookingNoLongerBlocksAvailability()`: Cancelled bookings free up the slot.
  - `testRejectionOfPastDate()`: Blocks past date reservations.
  - `testRejectionOfInvalidTimes()`: Blocks requests where end time $\le$ start time.
  - `testRejectionOfInactiveResource()`: Prevents booking deactivated facilities.
  - `testUserCannotCancelAnotherUserBooking()`: Security barrier for unauthorized cancellations.
  - `testAdminCanCancelAnyUserBooking()`: Verifies administrative override cancellation.
- **`UserServiceTest` (5 tests):**
  - Registration success & BCrypt hashing.
  - Faculty registration saves the selected role.
  - Public registration cannot create administrator accounts.
  - Duplicate email registration rejection.
  - Password confirmation mismatch rejection.
- **`ResourceServiceTest` (3 tests):**
  - Resource creation & retrieval.
  - Active/Inactive status toggle.
  - Multi-parameter search & filtering.
- **`SecurityAccessIntegrationTest` (5 tests):**
  - Unauthenticated access redirect to `/login`.
  - Student access to `/admin/**` returns **403 Forbidden**.
  - Faculty can use booking features but cannot access administration.
  - Administrator access to `/admin/**` returns **200 OK**.
- **`WebFlowIntegrationTest` (11 tests):**
  - Public landing page, login page, registration page.
  - Student and faculty registration POST flows.
  - Authenticated student dashboard, catalog, and My Bookings views.
  - Administrator dashboard, all bookings, and new resource form views.

---

## 12. Manual Demonstration Checklist

Use this checklist during your project viva or evaluation:

- [ ] **1. Public Discovery:** Navigate to `http://localhost:8081/`. Observe the landing page explaining the system and default evaluation accounts.
- [ ] **2. Student/Faculty Registration:** Click **Register**, select Student or Faculty, enter a name, email (`jordan@college.edu`), and password. Submit and verify redirection to login with the success alert.
- [ ] **3. Student Sign In:** Log in as `student@slotwise.edu` / `Student@1234`. Observe redirection to `/dashboard`.
- [ ] **4. Facility Browsing & Search:** Click **Browse Resources**. Test the keyword filter (e.g., search "Lab") and category dropdown.
- [ ] **5. Date & Time Availability Inspection:** Filter by tomorrow's date and a time slot (`10:00` to `11:00`). Verify available resources update dynamically.
- [ ] **6. Schedule Inspector:** Click **View Schedule** on `Seminar Room A`. Select a date to observe any currently confirmed bookings.
- [ ] **7. Make a Valid Booking:** Click **Book Slot** on `Seminar Room A` for tomorrow from `10:00` to `11:00`. Enter purpose "Group Presentation Review" and submit. Verify redirection to **My Bookings** with a green success alert.
- [ ] **8. Conflict Engine Verification (Overlapping Slot):** Attempt to book `Seminar Room A` again for the same date from `10:30` to `11:30`.
  - **Expected Result:** Application rejects the submission and displays a clear error alert indicating the exact overlapping time slot.
- [ ] **9. Adjacent Slot Verification:** Attempt to book `Seminar Room A` for the same date from `11:00` to `12:00`.
  - **Expected Result:** Application accepts and confirms the reservation without conflict.
- [ ] **10. Booking Cancellation:** On the **My Bookings** page, click **Cancel Booking**. Confirm the browser alert. Observe the status updates to `Cancelled`.
- [ ] **11. Slot Re-availability:** Re-attempt the booking for `Seminar Room A` from `10:00` to `11:00`.
  - **Expected Result:** Booking now succeeds because the previous reservation was cancelled.
- [ ] **12. RBAC Verification:** While logged in as student, try navigating directly to `/admin/dashboard`.
  - **Expected Result:** Spring Security denies access (**403 Forbidden**).
- [ ] **13. Administrator Control Center:** Sign out and log in as `admin@slotwise.edu` / `Admin@1234`.
  - Automatically redirected to `/admin/dashboard`.
  - View real-time statistics cards (Active Resources, Total Bookings, Upcoming, Cancelled).
- [ ] **14. Facility Management:** Navigate to **Manage Resources**. Click **Add New Resource**, create "Robotics Lab 3", and save. Toggle status from Active to Deactivate and back.
- [ ] **15. Administrative Booking Override:** Navigate to **Manage Bookings**. View all student reservations and test administrative revocation.
- [ ] **16. Persistence Across Restart:** Stop the application (`Ctrl+C`), restart it, and verify that user registrations, resources, and bookings remain intact in the database.

---

## 13. Concurrency Protection & Limitations Analysis

### Concurrency Protection Approach
In `BookingService.java`, the `createBooking` method is protected with `@Transactional(isolation = Isolation.READ_COMMITTED)` and thread synchronization. Under MySQL (InnoDB default transaction isolation level is **REPEATABLE READ**), queries within the transaction maintain read consistency. Additionally, an index (`idx_booking_conflict`) on `(resource_id, booking_date, status, start_time, end_time)` ensures high-speed lookups during concurrent booking attempts.

### Limitations of Single-Application Approach
- **Multi-Instance / Clustered Deployments:** Thread-level synchronization (`synchronized`) operates within a single JVM process. When scaled horizontally across multiple Kubernetes pods or behind a load balancer, concurrent requests hitting two different pods simultaneously could still theoretically experience a race condition without distributed locking.
- **Production Recommendation for Phase 2:** For multi-replica clustered environments, implement **database-level pessimistic write locking** (`SELECT ... FOR UPDATE` via `@Lock(LockModeType.PESSIMISTIC_WRITE)`) or distributed locks via Redis Redlock.

---

## 14. Common Errors & Troubleshooting

| Issue | Root Cause | Solution |
|---|---|---|
| `CommunicationsException: Communications link failure` | MySQL server is not running on port 3306. | Start MySQL with `net start MySQL80` or use `--spring.profiles.active=local`. |
| `Access denied for user 'root'@'localhost'` | Incorrect MySQL password in configuration. | Pass `DB_PASSWORD=your_password` environment variable or update `application.properties`. |
| `Port 8081 was already in use` | Another program or previous run is bound to port 8081. | Kill the process or launch with custom port: `java -jar target/slotwise-1.0.0.jar --server.port=9090`. |
| `Unknown database 'slotwise_db'` | Database has not been created yet in MySQL. | Run `CREATE DATABASE slotwise_db;` in MySQL Workbench or execute `database-setup.sql`. |

---

## 15. Next Steps for DevOps Pipeline Integration (Phase 2)

Now that the core SlotWise application is fully functional, persistent, and covered with automated tests, the following DevOps roadmap can be implemented in the next phase:

1. **Version Control & GitHub Workflow:**
   - Initialize Git repository and commit code.
   - Configure GitHub Actions for automated `mvn test` and `mvn compile` on every Pull Request.
2. **Containerization with Docker:**
   - Create a multi-stage `Dockerfile` (`eclipse-temurin:21-jdk-alpine` for build, `eclipse-temurin:21-jre-alpine` for production image).
   - Write `docker-compose.yml` linking the Spring Boot application container with a MySQL 8 container and persistent named volumes.
3. **CI/CD Pipeline with Jenkins:**
   - Write a `Jenkinsfile` with declarative pipeline stages: *Checkout*, *Unit & Integration Tests*, *SonarQube Code Quality Analysis*, *Docker Image Build*, and *Artifact Archival*.
4. **Orchestration with Kubernetes (K8s):**
   - Author Kubernetes deployment manifests (`deployment.yaml`, `service.yaml`, `configmap.yaml`, `secret.yaml`, `pvc.yaml` for MySQL storage).
   - Configure health probes (`livenessProbe` and `readinessProbe`) leveraging Spring Boot Actuator.
