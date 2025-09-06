# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FedUWAComm is a monorepo containing three main modules for underwater acoustic communication optimization using federated learning:
- **Python VM Module** (`python-vm/`): Core ML algorithms, BELLHOP simulation, feature extraction
- **Spring Boot Backend** (`backend-springboot/`): RESTful API service with multi-module Maven architecture
- **Admin Frontend** (`frontend-admin/`): Management UI (currently undeveloped)

## Common Development Commands

### Python Virtual Machine Module

```bash
# Install dependencies and module
cd python-vm
pip install -r requirements.txt
pip install -e .

# Run complete ML workflow
python scripts/complete_workflow.py

# Verify database
python scripts/verify_database.py

# Run tests
python -m pytest tests/

# Run specific test file
python -m pytest tests/test_database.py
```

### Spring Boot Backend

```bash
# Start development server
cd backend-springboot
mvn -pl feduwacomm-server spring-boot:run

# Or use the Windows batch script
run.bat

# Build project
mvn clean package

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=UserControllerTest

# Run specific test method
mvn test -Dtest=UserControllerTest#testLogin_Success

# Test specific module suite
mvn test -Dtest=UserModuleTestSuite
mvn test -Dtest=AdminModuleTestSuite
```

## Architecture and Key Components

### Multi-Module Maven Structure
The Spring Boot backend uses a multi-module Maven architecture:
- `feduwacomm-parent`: Parent POM with dependency management
- `feduwacomm-common`: Shared utilities, constants, and common components
- `feduwacomm-pojo`: Data objects (entities, DTOs)
- `feduwacomm-server`: Main application with controllers, services, and mappers

### Python Module Architecture
The Python VM module is organized as a proper Python package:
- `src/feduwacomm/`: Main package
  - `core/`: Core functionality
  - `ml/`: Machine learning models (RandomForest trainer, feature extractor, evaluator)
  - `database/`: Database operations (MySQL via PyMySQL)
  - `acoustic/`: BELLHOP simulation and environment generation
  - `utils/`: Utility functions

### Database Configuration
- **MySQL Database**: `feduwacomm` (localhost:3306)
- **Python VM**: Uses `.env` file for database credentials
- **Spring Boot**: Configuration in `application.yml`
  - Default password: NBNB (should be changed in production)
  - Database tables are designed with JSON fields for flexibility

### Authentication & Security
- **JWT-based authentication** with configurable secret and expiration
- **Spring Security** for endpoint protection
- **User permission system** with fine-grained access control
- **API prefix**: `/api` for all REST endpoints

## API Structure

### Base URL: `http://localhost:8080/api`

Key API modules documented in `docs/shared/api/HTTP/`:
- User Management API (`user-api-reference.md`)
- Admin Management API (`admin-api-reference.md`)
- Federated Task API (`federated-task-api-reference.md`)
- Model Version API (`model-version-api-reference.md`)
- Training Data API (`training-data-api-reference.md`)
- System Log API (`system-log-api-reference.md`)

### Response Format
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

## Testing Approach

### Spring Boot Testing
- Unit tests for controllers, services, and utilities
- Integration tests for API endpoints
- Test suites organized by module (UserModuleTestSuite, AdminModuleTestSuite)
- MockMvc for controller testing
- Test data uses in-memory repositories

### Python Testing
- pytest framework for unit and integration tests
- Test coverage for database operations, ML models, and utilities
- Mock database connections for isolated testing

## Development Workflow

1. **Feature Development**:
   - Create feature branch from `develop`
   - Implement changes following existing code patterns
   - Write unit tests for new functionality
   - Run tests locally before committing

2. **Database Changes**:
   - Update schema documentation in `docs/shared/database/database_schema.md`
   - Create migration scripts if needed
   - Update both Python and Spring Boot configurations

3. **API Development**:
   - Controllers in `com.feduwacomm.controller`
   - Services in `com.feduwacomm.service`
   - MyBatis mappers in `com.feduwacomm.mapper`
   - Update API documentation in `docs/shared/api/`

## Current Development Status

- **Python VM**: ✅ Fully functional with ML pipeline
- **Spring Boot Backend**: 🚧 Framework complete, business logic in development
- **Frontend Admin**: 📋 Not yet started

## Important Configuration Files

- `backend-springboot/feduwacomm-server/src/main/resources/application.yml`: Spring Boot configuration
- `python-vm/.env`: Python database credentials (create from template)
- `backend-springboot/pom.xml`: Maven parent POM with dependency versions
- `python-vm/requirements.txt`: Python dependencies
- `python-vm/setup.py`: Python package configuration

## Key Technologies

- **Backend**: Spring Boot 3.4.4, MyBatis 3.0.3, MySQL, JWT, Lombok
- **Python**: scikit-learn, pandas, numpy, PyMySQL, BELLHOP
- **Java Version**: 17
- **Python Version**: 3.8+
- **Build Tool**: Maven 3.6+