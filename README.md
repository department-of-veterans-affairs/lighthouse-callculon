# Callculon

Callculon is robot that is responsible for invoking an HTTP endpoint.
It is designed to work with AWS Events.
It was created specifically to process scheduled events as part of a timer solution.

Callculon provides Slack integration and notify Slack upon success or failure
and supports sensitive data via SecureString entries in AWS Parameter Store.

See [Health API Deployer](https://github.com/department-of-veterans-affairs/health-apis-deployer).

## Lambda Configuration

The following environment variables can be used to configure the Callculon lambda.

**`CALLCULON_CONNECT_TIMEOUT`** _`(PT20S)`_  
How long to wait before giving up when connecting to a remote server
specified as an ISO 8601 duration.

**`CALLCULON_REQUEST_TIMEOUT`**  _`(PT120S)`_  
How long to wait before giving up on a server to respond to a request
specified as an ISO 8601 duration.

## Invoking

Callculon is invoked with the following input JSON structure.
See [payload.json](payload.json)

```
{
  name: ........... [String] Name of this invocation used in notifications and logging.
  deployment: {     [Object] Informational only, no action taken regardless of any values.
    enabled: ...... [Boolean] Whether the source timer was enabled.
    environment ... [String] The environment name this timer is deployed.
    cron: ......... [Cron] Time schedule.
    product: ...... [String] Production name.
    version: ...... [String] Production deployment version.
    id: ........... [String] Deployment ID.
  }
  request: {        [Object] Request configuration
    protocol: ..... [enum] (HTTP|HTTPS) Protocol use when making HTTP request.
    hostname: ..... [String] HTTP server host name.
    port: ......... [Integer] HTTP server port.
    path: ......... [Secret String] HTTP request path.
    method: ....... [enum] (GET) HTTP method. Note: POST support to be added later.
    headers: {      [Object] Optional dictionary of HTTP request headers.
       [String]: [Secret String] Any key value pair.
     }
  }
  notification: {   [Object] Configuration for all notifications.
    slack: {        [Object] Configuration for Slack notifications.
      webhook: .... [Secret URL] Slack web hook URL.
      channel: .... [String] Slack channel without the leading `#`
      onFailure: .. [Boolean] Whether notification should be sent on failure. (true)
      onSuccess: .. [Boolean] Whether notification should be sent on success. (false)
    }
  }
}
```

### Secrets
Callculon support secrets in the input configuration object backed by AWS Parameter Store.
The value of the following fields support secrets.
- `request.path`
- `request.headers.*`
- `notification.slack.webhook`

Syntax Rules

- Defined by `aws-secret(${name})` where `${name}` is an AWS Parameter Store name
- AWS parameter must be Secure String type
- Secrets may appear multiple times in the value
- Secrets may only be specified in configuration values
- Secrets cannot span multiple lines
- Secrets cannot be nested

Example
```
  "request": {
    ...
    "path": "/services/fhir/v0/dstu2/metadata",
    "headers": {
       "apikey": "aws-secret(/dvp/production/facilities/api-key)"
     }
  },
  "notification": {
    "slack": {
      "webhook":"aws-secret(/dvp/slack/liberty)",
      "channel":"shanktopus"
    }
```
