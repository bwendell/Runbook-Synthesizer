# aws-adapters Specification Delta

## ADDED Requirements

### Requirement: AwsSnsAlertSourceAdapter

The system SHALL provide an `AwsSnsAlertSourceAdapter` that parses CloudWatch Alarm notifications delivered via SNS webhook.

#### Scenario: Source type identification
**Given** an AwsSnsAlertSourceAdapter instance
**When** `sourceType()` is called
**Then** it returns `"aws-cloudwatch-sns"`

#### Scenario: Detect SNS CloudWatch Alarm message
**Given** an AwsSnsAlertSourceAdapter instance
**And** a JSON payload with `Type: "Notification"` and `TopicArn` containing `":sns:"`
**When** `canHandle(payload)` is called
**Then** it returns `true`

#### Scenario: Reject non-SNS payload
**Given** an AwsSnsAlertSourceAdapter instance
**And** a JSON payload without SNS structure
**When** `canHandle(payload)` is called
**Then** it returns `false`

#### Scenario: Parse ALARM state to CRITICAL severity
**Given** an AwsSnsAlertSourceAdapter instance
**And** a valid SNS-wrapped CloudWatch Alarm JSON with `NewStateValue: "ALARM"`
**When** `parseAlert(payload)` is called
**Then** it returns an Alert with `severity = CRITICAL`
**And** the Alert `title` equals the `AlarmName`
**And** the Alert `message` equals the `NewStateReason`

#### Scenario: Parse INSUFFICIENT_DATA state to WARNING severity
**Given** an AwsSnsAlertSourceAdapter instance
**And** a valid SNS-wrapped CloudWatch Alarm JSON with `NewStateValue: "INSUFFICIENT_DATA"`
**When** `parseAlert(payload)` is called
**Then** it returns an Alert with `severity = WARNING`

#### Scenario: Skip OK state
**Given** an AwsSnsAlertSourceAdapter instance
**And** a valid SNS-wrapped CloudWatch Alarm JSON with `NewStateValue: "OK"`
**When** `parseAlert(payload)` is called
**Then** it returns `null`

---

### Requirement: AwsBedrockLlmProvider

The system SHALL provide an `AwsBedrockLlmProvider` that implements `LlmProvider` using AWS Bedrock for text generation and embeddings.

#### Scenario: Provider identification
**Given** an AwsBedrockLlmProvider instance
**When** `providerId()` is called
**Then** it returns `"aws-bedrock"`

#### Scenario: Generate text with Claude 3 Haiku
**Given** an AwsBedrockLlmProvider configured with Claude 3 Haiku model
**And** a valid prompt string
**When** `generateText(prompt, config)` is called
**Then** it returns a CompletableFuture that completes with generated text
**And** the Bedrock request uses model ID `anthropic.claude-3-haiku-20240307-v1:0`

#### Scenario: Generate single embedding with Cohere
**Given** an AwsBedrockLlmProvider configured with Cohere Embed v3
**And** a text string to embed
**When** `generateEmbedding(text)` is called
**Then** it returns a CompletableFuture that completes with a float array
**And** the array has dimension matching Cohere Embed v3 output (1024)

#### Scenario: Generate batch embeddings
**Given** an AwsBedrockLlmProvider configured with Cohere Embed v3
**And** a list of 3 text strings
**When** `generateEmbeddings(texts)` is called
**Then** it returns a CompletableFuture that completes with a list of 3 float arrays
**And** each array has dimension 1024

---

### Requirement: OllamaLlmProvider (MVP Default)

The system SHALL provide an `OllamaLlmProvider` that implements `LlmProvider` using a local Ollama instance for development and testing.

#### Scenario: Provider identification
**Given** an OllamaLlmProvider instance
**When** `providerId()` is called
**Then** it returns `"ollama"`

#### Scenario: Generate text with local model
**Given** an OllamaLlmProvider configured with `llama3.2:3b` model
**And** an Ollama server running at the configured base URL
**When** `generateText(prompt, config)` is called
**Then** it returns a CompletableFuture that completes with generated text

#### Scenario: Generate embedding with local model
**Given** an OllamaLlmProvider configured with `nomic-embed-text` model
**And** an Ollama server running at the configured base URL
**When** `generateEmbedding(text)` is called
**Then** it returns a CompletableFuture that completes with a float array
**And** the array has dimension 768

#### Scenario: Graceful handling of Ollama offline
**Given** an OllamaLlmProvider instance
**And** no Ollama server is running
**When** any LLM operation is called
**Then** the CompletableFuture completes exceptionally with a clear error message

---

## MODIFIED Requirements

### Requirement: CloudAdapterFactory Provides All Adapter Types

The `CloudAdapterFactory` SHALL provide factory methods for all cloud adapter types.

#### Scenario: Get metrics adapter class for AWS
**Given** a CloudAdapterFactory configured with `cloud.provider: aws`
**When** `getMetricsAdapterClass()` is called
**Then** it returns `AwsCloudWatchMetricsAdapter.class`

#### Scenario: Get logs adapter class for AWS
**Given** a CloudAdapterFactory configured with `cloud.provider: aws`
**When** `getLogsAdapterClass()` is called
**Then** it returns `AwsCloudWatchLogsAdapter.class`

#### Scenario: Get alert source adapter class for AWS
**Given** a CloudAdapterFactory configured with `cloud.provider: aws`
**When** `getAlertSourceAdapterClass()` is called
**Then** it returns `AwsSnsAlertSourceAdapter.class`

#### Scenario: Get LLM provider class for AWS
**Given** a CloudAdapterFactory configured with `cloud.provider: aws`
**When** `getLlmProviderClass()` is called
**Then** it returns `AwsBedrockLlmProvider.class`
