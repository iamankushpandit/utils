// src/otel.js - OpenTelemetry Web Instrumentation
import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
import { getWebAutoInstrumentations } from '@opentelemetry/auto-instrumentations-web';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { SimpleSpanProcessor, BatchSpanProcessor } from '@opentelemetry/sdk-trace-base';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { ZoneContextManager } from '@opentelemetry/context-zone';
import { Resource } from '@opentelemetry/resources';
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions';

// 1. Configure the Exporter (sends traces to Collector)
const exporter = new OTLPTraceExporter({
  url: 'http://localhost:4318/v1/traces', // OTel Collector HTTP receiver
});

// 2. Configure the Provider
const provider = new WebTracerProvider({
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: 'utility-explorer-ui',
  }),
});

// 3. Register Span Processor
// Use BatchSpanProcessor for production, Simple for generic dev/debug
provider.addSpanProcessor(new BatchSpanProcessor(exporter));

// 4. Register the provider
provider.register({
  contextManager: new ZoneContextManager(),
});

// 5. Register Auto-Instrumentations (Fetch, XHR, Doc Load)
registerInstrumentations({
  instrumentations: [
    getWebAutoInstrumentations({
      '@opentelemetry/instrumentation-fetch': {
        propagateTraceHeaderCorsUrls: [
            /.+/g, // Include standard trace headers in all fetch requests
        ],
      },
      '@opentelemetry/instrumentation-xml-http-request': {
        propagateTraceHeaderCorsUrls: [
            /.+/g, 
        ],
      },
    }),
  ],
});

console.log('OpenTelemetry initialized for frontend');
