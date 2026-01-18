# WireMock Directory Structure

This directory contains WireMock mappings and response files for integration tests.

## Directory Structure

```
wiremock/
├── mappings/      # Stub definitions (JSON files)
└── __files/       # Response body files (JSON, text, etc.)
```

## Usage

Integration tests in this project use WireMock programmatically via the `IntegrationTestBase` class.
For complex or reusable stubs, you can define them as JSON files in the `mappings/` directory.

### Example Mapping File

Create a file like `mappings/oci-monitoring-success.json`:

```json
{
  "request": {
    "method": "POST",
    "urlPathPattern": "/monitoring/.*"
  },
  "response": {
    "status": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "bodyFileName": "monitoring-response.json"
  }
}
```

Then place the response body in `__files/monitoring-response.json`.

## Documentation

- [WireMock Documentation](https://wiremock.org/docs/)
- [Stubbing](https://wiremock.org/docs/stubbing/)
- [Response Templating](https://wiremock.org/docs/response-templating/)
