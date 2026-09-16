# JWT login (step 2)

POST /workmind/auth/login accepts email and password. After BCrypt verification,
it returns HTTP 200 with {"accessToken":"<JWT>"}.
Invalid credentials still return HTTP 401.

Access tokens are signed with HS256 and contain sub (member ID), memberId,
email, role, iat and exp. The default lifetime is 1800 seconds (30 minutes).
JwtTokenProvider.parseAndValidate verifies the signature and expiration and
returns the verified claims; invalid tokens raise an exception.

## Configuration

application.properties defines:
- jwt.secret=${JWT_SECRET}
- jwt.access-token-expiration-seconds=${JWT_ACCESS_TOKEN_EXPIRATION_SECONDS:1800}

JWT_SECRET must be a Base64-encoded cryptographically random secret of at least
32 bytes. No default production secret is provided. Supply a stable secret to
the application process; changing it invalidates existing tokens.

For a local PowerShell session, generate a secret without printing it:

```powershell
$jwtKeyBytes = New-Object byte[] 32
$jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtKeyBytes)
$jwtRng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtKeyBytes)
$env:JWT_ACCESS_TOKEN_EXPIRATION_SECONDS = '1800'
.\gradlew.bat bootRun
```

When launching from an IDE, configure these environment variables in its run
configuration instead. Keep the secret outside version control.

Step 3 adds Bearer-token request authentication through JwtAuthenticationFilter.
See [JWT request authentication](jwt-request-authentication.md) for SecurityContext
handling and Postman tests. Refresh Tokens and logout are not implemented.

## Verification

Run `gradlew.bat test`. Tests generate a separate in-memory signing key and
do not require JWT_SECRET. The existing application context test uses the
configured MySQL connection.
## Local server startup

The project optionally imports ./jwt-local.properties from its working directory.
This Git-ignored file contains a generated local-only key and is not packaged in
the application JAR. JWT_SECRET overrides this local key when supplied.
Start the server with WorkMind_BE as the working directory (including in STS/IDE).
The current local key persists across restarts. Do not deploy this local file.
Other checkouts and deployments must supply JWT_SECRET as described above.