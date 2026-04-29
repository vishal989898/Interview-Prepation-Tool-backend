# Interview AI Backend - Spring Boot MySQL

This is the Spring Boot backend for the Interview AI application, which generates interview reports based on user resumes, self-descriptions, and job descriptions using Google's Gemini AI.

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.3**
- **Spring Data JPA**
- **MySQL Database**
- **Spring Security with JWT**
- **Google Gemini AI API**
- **Apache PDFBox** (for PDF parsing)
- **OpenHTMLtoPDF** (for PDF generation)

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- Google AI API Key

## Setup Instructions

### 1. Database Setup

The application will automatically create the database `interview_ai_db` when it first runs if it doesn't exist. Alternatively, you can create it manually:

```sql
CREATE DATABASE interview_ai_db;
```

### 2. Configuration

Create a `.env` file or update `application.properties` in `src/main/resources/`:

```properties
# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/interview_ai_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=vishal

# Google AI API Key
google.genai.api.key=YOUR_API_KEY_HERE

# JWT Secret (optional - default is set)
jwt.secret=your_secret_key_here
```

### 3. Build the Application

Navigate to the backend directory and run:

```bash
cd Backend-SpringBoot
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

The server will start on `http://localhost:8181`

### 5. Get Your Google AI API Key

1. Visit [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Create an API key
3. Add it to your `.env` file or `application.properties`

## API Endpoints

### Authentication

- **POST** `/api/auth/register` - Register a new user
- **POST** `/api/auth/login` - Login user
- **GET** `/api/auth/logout` - Logout user
- **GET** `/api/auth/get-me` - Get current user details

### Interview

- **POST** `/api/interview/` - Generate interview report (multipart/form-data)
- **GET** `/api/interview/report/{interviewId}` - Get interview report by ID
- **GET** `/api/interview/` - Get all interview reports for logged-in user
- **POST** `/api/interview/resume/pdf/{interviewReportId}` - Generate resume PDF

## Frontend Integration

The frontend is already configured to connect to this backend at `http://localhost:8181`. Make sure:

1. The Spring Boot backend is running on port 8181
2. CORS is enabled for `http://localhost:5173` (Vite dev server)
3. Cookies are enabled for authentication

## Project Structure

```
Backend-SpringBoot/
├── src/
│   ├── main/
│   │   ├── java/com/interview/
│   │   │   ├── config/          # Security and Web configuration
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # JPA Entities
│   │   │   ├── repository/      # Data repositories
│   │   │   ├── security/        # JWT utilities and filters
│   │   │   └── service/         # Business logic services
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── pom.xml
```

## Database Schema

The application creates the following tables:

- **users** - User accounts
- **blacklist_tokens** - Revoked JWT tokens
- **interview_reports** - Generated interview reports
- **technical_questions** - Technical questions (embedded in reports)
- **behavioral_questions** - Behavioral questions (embedded in reports)
- **skill_gaps** - Skill gaps (embedded in reports)
- **preparation_plans** - Preparation plans (embedded in reports)

## Troubleshooting

### Port Already in Use

If port 8181 is already in use, change it in `application.properties`:

```properties
server.port=YOUR_PORT
```

Don't forget to update the frontend's API base URL as well.

### Database Connection Error

Ensure MySQL is running and credentials in `application.properties` are correct.

### AI API Error

Make sure you have a valid Google AI API key and internet connection.

## Development

To run with hot reload:

```bash
mvn spring-boot:run
```

The application will automatically restart when code changes are detected.

## License

ISC
